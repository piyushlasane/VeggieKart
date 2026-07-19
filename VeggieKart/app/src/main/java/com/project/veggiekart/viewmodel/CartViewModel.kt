package com.project.veggiekart.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.project.veggiekart.model.ProductModel
import com.project.veggiekart.repository.CartRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CartItem(
    val product: ProductModel,
    val quantity: Long
)

data class CartState(
    val items: List<CartItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalAmount: Double = 0.0,
    val totalItems: Int = 0,
    // Products that were in the cart but couldn't be loaded (deleted/broken doc),
    // surfaced to the UI instead of silently vanishing.
    val unavailableProductIds: List<String> = emptyList()
)

/**
 * IMPORTANT: This ViewModel must be obtained with an Activity-scoped owner
 * everywhere it's used, e.g.:
 *   viewModel(LocalContext.current as ComponentActivity)
 * instead of the default viewModel(), so every screen shares the same
 * instance/state instead of each navigation destination getting its own
 * empty copy that has to reload the cart from scratch.
 */
class CartViewModel(
    private val repository: CartRepository = CartRepository()
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    var showSnackbar by mutableStateOf<String?>(null)
        private set

    // Debounces rapid +/- taps per product so we don't fire a Firestore write
    // on every single tap - only after taps settle for QUANTITY_WRITE_DEBOUNCE_MS.
    private val pendingWrites = mutableMapOf<String, Job>()

    init {
        loadCart()
    }

    /** Full reload from Firestore. Call this on initial load / login change / pull-to-refresh only -
     * NOT after every cart mutation (those update local state optimistically instead). */
    fun loadCart() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _cartState.value = CartState()
            return
        }

        viewModelScope.launch {
            try {
                _cartState.value = _cartState.value.copy(isLoading = true, error = null)

                val cartMap = repository.getCartMap(uid)
                if (cartMap.isEmpty()) {
                    _cartState.value = CartState(isLoading = false)
                    return@launch
                }

                val result = repository.resolveProducts(cartMap)

                val items = result.items.map { (_, pair) -> CartItem(pair.first, pair.second) }
                val totalAmount = items.sumOf {
                    (it.product.actualPrice.toDoubleOrNull() ?: 0.0) * it.quantity
                }
                val totalItems = items.sumOf { it.quantity.toInt() }

                _cartState.value = CartState(
                    items = items,
                    isLoading = false,
                    totalAmount = totalAmount,
                    totalItems = totalItems,
                    unavailableProductIds = result.unavailableProductIds,
                    error = if (result.unavailableProductIds.isNotEmpty())
                        "${result.unavailableProductIds.size} item(s) in your cart are no longer available"
                    else null
                )

            } catch (e: Exception) {
                _cartState.value = _cartState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Failed to load cart"
                )
            }
        }
    }

    /** Adds one unit of [product] to the cart. Updates UI instantly (optimistic),
     * writes to Firestore in the background, and rolls back only if the write fails. */
    fun addToCart(product: ProductModel, onResult: (Boolean, String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Please login to add items to cart")
            return
        }

        pendingWrites[product.id]?.cancel()

        val previousState = _cartState.value
        val existing = previousState.items.find { it.product.id == product.id }
        val updatedItems = if (existing != null) {
            previousState.items.map {
                if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
            }
        } else {
            previousState.items + CartItem(product, 1)
        }
        applyLocalItems(previousState, updatedItems)

        viewModelScope.launch {
            try {
                repository.incrementItem(uid, product.id)
                onResult(true, "Added to cart")
            } catch (e: Exception) {
                _cartState.value = previousState
                onResult(false, e.localizedMessage ?: "Failed to add to cart")
            }
        }
    }

    /** Sets [productId]'s quantity to [newQuantity]. Updates UI instantly and debounces
     * the Firestore write so rapid repeated +/- taps only trigger one network write. */
    fun updateQuantity(productId: String, newQuantity: Long, onResult: (Boolean, String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Please login first")
            return
        }

        if (newQuantity < 1) {
            removeFromCart(productId, onResult)
            return
        }

        val previousState = _cartState.value
        val updatedItems = previousState.items.map {
            if (it.product.id == productId) it.copy(quantity = newQuantity) else it
        }
        applyLocalItems(previousState, updatedItems)

        pendingWrites[productId]?.cancel()
        pendingWrites[productId] = viewModelScope.launch {
            delay(QUANTITY_WRITE_DEBOUNCE_MS)
            try {
                repository.setItemQuantity(uid, productId, newQuantity)
                onResult(true, "Quantity updated")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Failed to update quantity")
            }
        }
    }

    /** Removes [productId] from the cart. Updates UI instantly, rolls back on failure. */
    fun removeFromCart(productId: String, onResult: (Boolean, String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Please login first")
            return
        }

        pendingWrites[productId]?.cancel()

        val previousState = _cartState.value
        val updatedItems = previousState.items.filterNot { it.product.id == productId }
        applyLocalItems(previousState, updatedItems)

        viewModelScope.launch {
            try {
                repository.removeItem(uid, productId)
                onResult(true, "Removed from cart")
            } catch (e: Exception) {
                _cartState.value = previousState
                onResult(false, e.localizedMessage ?: "Failed to remove item")
            }
        }
    }

    fun clearCart(onResult: (Boolean, String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Please login first")
            return
        }

        pendingWrites.values.forEach { it.cancel() }
        pendingWrites.clear()

        val previousState = _cartState.value
        _cartState.value = CartState(isLoading = false)

        viewModelScope.launch {
            try {
                repository.clearCart(uid)
                onResult(true, "Cart cleared")
            } catch (e: Exception) {
                _cartState.value = previousState
                onResult(false, e.localizedMessage ?: "Failed to clear cart")
            }
        }
    }

    fun dismissSnackbar() {
        showSnackbar = null
    }

    // Helper function to check if product is in cart
    fun isInCart(productId: String): Boolean {
        return _cartState.value.items.any { it.product.id == productId }
    }

    // Helper function to get quantity of product in cart
    fun getProductQuantity(productId: String): Long {
        return _cartState.value.items.find { it.product.id == productId }?.quantity ?: 0L
    }

    /** Recomputes totals from [items] and pushes them into state, keeping [previousState]'s
     * other fields (like unavailableProductIds) untouched. */
    private fun applyLocalItems(previousState: CartState, items: List<CartItem>) {
        _cartState.value = previousState.copy(
            items = items,
            isLoading = false,
            totalAmount = items.sumOf { (it.product.actualPrice.toDoubleOrNull() ?: 0.0) * it.quantity },
            totalItems = items.sumOf { it.quantity.toInt() }
        )
    }

    companion object {
        private const val QUANTITY_WRITE_DEBOUNCE_MS = 400L
    }
}