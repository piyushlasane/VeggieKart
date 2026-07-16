package com.project.veggiekart.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.veggiekart.model.OrderItemModel
import com.project.veggiekart.network.CreateOrderRequest
import com.project.veggiekart.network.OrderItemDto
import com.project.veggiekart.network.RetrofitClient
import com.project.veggiekart.network.VerifyPaymentRequest
import com.project.veggiekart.payment.RazorpayLauncher
import com.project.veggiekart.payment.RazorpayResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class CheckoutState {
    data object Idle : CheckoutState()
    data object CreatingOrder : CheckoutState()
    data object AwaitingPayment : CheckoutState()
    data object VerifyingPayment : CheckoutState()
    data class Success(val orderId: String) : CheckoutState()
    data class Failed(val message: String) : CheckoutState()
}

class OrderViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

    fun startCheckout(
        activity: Activity,
        cartItems: List<CartItem>,
        totalAmount: Double,
        addressId: String,
        userName: String,
        userEmail: String,
        userPhone: String
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _checkoutState.value = CheckoutState.Failed("You need to be signed in to check out.")
            return
        }

        viewModelScope.launch {
            try {
                _checkoutState.value = CheckoutState.CreatingOrder
                val orderResponse = RetrofitClient.paymentApi.createOrder(
                    CreateOrderRequest(amountInRupees = totalAmount, userId = uid)
                )

                _checkoutState.value = CheckoutState.AwaitingPayment
                RazorpayLauncher.open(
                    activity = activity,
                    keyId = orderResponse.razorpayKeyId,
                    razorpayOrderId = orderResponse.razorpayOrderId,
                    amountInPaise = orderResponse.amountInPaise,
                    userName = userName,
                    userEmail = userEmail,
                    userPhone = userPhone
                )

                when (val result = RazorpayLauncher.awaitResult()) {
                    is RazorpayResult.Failure -> {
                        _checkoutState.value = CheckoutState.Failed(result.description)
                    }
                    is RazorpayResult.Success -> {
                        _checkoutState.value = CheckoutState.VerifyingPayment
                        verifyAndSaveOrder(uid, addressId, cartItems, result)
                    }
                }
            } catch (e: Exception) {
                _checkoutState.value = CheckoutState.Failed(e.message ?: "Checkout failed")
            }
        }
    }

    private suspend fun verifyAndSaveOrder(
        uid: String,
        addressId: String,
        cartItems: List<CartItem>,
        result: RazorpayResult.Success
    ) {
        try {
            val verifyResponse = RetrofitClient.paymentApi.verifyPayment(
                VerifyPaymentRequest(
                    razorpayOrderId = result.orderId,
                    razorpayPaymentId = result.paymentId,
                    razorpaySignature = result.signature,
                    userId = uid,
                    addressId = addressId,
                    items = cartItems.map {
                        OrderItemDto(
                            productId = it.product.id,
                            name = it.product.title,
                            quantity = it.quantity.toInt(),
                            price = it.product.actualPrice.toDoubleOrNull() ?: 0.0
                        )
                    }
                )
            )

            if (!verifyResponse.verified) {
                _checkoutState.value = CheckoutState.Failed(
                    verifyResponse.message ?: "Payment could not be verified"
                )
                return
            }

            // Backend already wrote the source-of-truth order to Firestore's
            // top-level "orders" collection after verifying the signature.
            // Mirror a lightweight copy under the user for a fast "My Orders" read,
            // and clear the cart now that the order is confirmed.
            val orderItems = cartItems.map {
                OrderItemModel(
                    productId = it.product.id,
                    title = it.product.title,
                    price = it.product.actualPrice,
                    quantity = it.quantity
                )
            }
            val userOrderRef = firestore.collection("users").document(uid)
                .collection("orders").document(verifyResponse.orderId ?: result.orderId)

            userOrderRef.set(
                mapOf(
                    "id" to (verifyResponse.orderId ?: result.orderId),
                    "items" to orderItems,
                    "addressId" to addressId,
                    "razorpayOrderId" to result.orderId,
                    "razorpayPaymentId" to result.paymentId,
                    "status" to "placed",
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()

            firestore.collection("users").document(uid)
                .update("cartItems", emptyMap<String, Long>())
                .await()

            _checkoutState.value = CheckoutState.Success(verifyResponse.orderId ?: result.orderId)
        } catch (e: Exception) {
            _checkoutState.value = CheckoutState.Failed(
                "Payment succeeded but saving the order failed: ${e.message}. Contact support with payment id ${result.paymentId}."
            )
        }
    }

    fun resetCheckoutState() {
        _checkoutState.value = CheckoutState.Idle
    }
}
