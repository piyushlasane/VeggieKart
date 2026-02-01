package com.project.veggiekart.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.veggiekart.model.ProductModel
import com.project.veggiekart.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CartItem(
    val product: ProductModel,
    val quantity: Long
)

data class CartState(
    val items: List<CartItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalAmount: Double = 0.0,
    val totalItems: Int = 0
)

class CartViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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

                // Get user document
                val userDoc = firestore.collection("users").document(uid).get().await()
                val user = userDoc.toObject(UserModel::class.java)
                val cartItems = user?.cartItems ?: emptyMap()

                if (cartItems.isEmpty()) {
                    _cartState.value = CartState(isLoading = false)
                    return@launch
                }

                // Fetch product details for each cart item
                val items = mutableListOf<CartItem>()
                for ((productId, quantity) in cartItems) {
                    try {
                        val productDoc = firestore.collection("data")
                            .document("stock")
                            .collection("products")
                            .document(productId)
                            .get()
                            .await()

                        val product = productDoc.toObject(ProductModel::class.java)
                        if (product != null) {
                            items.add(CartItem(product, quantity))
                        }
                    } catch (e: Exception) {
                        // Skip this product if fetch fails
                        continue
                    }
                }

                // Calculate totals
                val totalAmount = items.sumOf {
                    (it.product.actualPrice.toDoubleOrNull() ?: 0.0) * it.quantity
                }
                val totalItems = items.sumOf { it.quantity.toInt() }

                _cartState.value = CartState(
                    items = items,
                    isLoading = false,
                    totalAmount = totalAmount,
                    totalItems = totalItems
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
                val userDoc = firestore.collection("users").document(uid)
                val snapshot = userDoc.get().await()
                val user = snapshot.toObject(UserModel::class.java)
                val currentCart = user?.cartItems?.toMutableMap() ?: mutableMapOf()

                // Increment quantity or add new item
                val currentQty = currentCart[productId] ?: 0L
                currentCart[productId] = currentQty + 1

                // Update Firestore
                userDoc.update("cartItems", currentCart).await()

                // Reload cart
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
                val userDoc = firestore.collection("users").document(uid)
                val snapshot = userDoc.get().await()
                val user = snapshot.toObject(UserModel::class.java)
                val currentCart = user?.cartItems?.toMutableMap() ?: mutableMapOf()

                currentCart[productId] = newQuantity

                userDoc.update("cartItems", currentCart).await()
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
                val userDoc = firestore.collection("users").document(uid)
                val snapshot = userDoc.get().await()
                val user = snapshot.toObject(UserModel::class.java)
                val currentCart = user?.cartItems?.toMutableMap() ?: mutableMapOf()

                currentCart.remove(productId)

                userDoc.update("cartItems", currentCart).await()
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
                firestore.collection("users")
                    .document(uid)
                    .update("cartItems", emptyMap<String, Long>())
                    .await()

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