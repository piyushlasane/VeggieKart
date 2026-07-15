package com.project.veggiekart.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.project.veggiekart.model.ProductModel
import com.project.veggiekart.repository.CartRepository
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

class CartViewModel(
    private val repository: CartRepository = CartRepository()
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    var showSnackbar by mutableStateOf<String?>(null)
        private set

    init {
        loadCart()
    }

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

    fun addToCart(productId: String, onResult: (Boolean, String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Please login to add items to cart")
            return
        }

        viewModelScope.launch {
            try {
                repository.incrementItem(uid, productId)
                loadCart()
                onResult(true, "Added to cart")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Failed to add to cart")
            }
        }
    }

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

        viewModelScope.launch {
            try {
                repository.setItemQuantity(uid, productId, newQuantity)
                loadCart()
                onResult(true, "Quantity updated")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Failed to update quantity")
            }
        }
    }

    fun removeFromCart(productId: String, onResult: (Boolean, String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(false, "Please login first")
            return
        }

        viewModelScope.launch {
            try {
                repository.removeItem(uid, productId)
                loadCart()
                onResult(true, "Removed from cart")
            } catch (e: Exception) {
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

        viewModelScope.launch {
            try {
                repository.clearCart(uid)
                loadCart()
                onResult(true, "Cart cleared")
            } catch (e: Exception) {
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
}