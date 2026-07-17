package com.project.veggiekart.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.veggiekart.model.AddressModel
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

    /**
     * The payment itself succeeded at Razorpay, but confirming that with our
     * backend failed even after retries (e.g. backend was slow to wake up).
     * Crucially: the "Pay Now" button must NOT be shown again in this state,
     * since that would charge the user a second time for the same cart.
     * Only a "Retry Verification" action (re-calling /verify, never
     * /create-order again) is safe here.
     */
    data class PaymentSucceededPendingVerification(val paymentId: String) : CheckoutState()
}

class OrderViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

    // Remembers the details of a payment that succeeded at Razorpay but
    // couldn't be confirmed with our backend yet, so "Retry Verification"
    // can safely re-run just that step without charging the user again.
    private data class PendingVerification(
        val uid: String,
        val addressId: String,
        val address: AddressModel?,
        val cartItems: List<CartItem>,
        val result: RazorpayResult.Success
    )
    private var pendingVerification: PendingVerification? = null

    fun startCheckout(
        activity: Activity,
        cartItems: List<CartItem>,
        totalAmount: Double,
        address: AddressModel,
        userName: String,
        userEmail: String,
        userPhone: String
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _checkoutState.value = CheckoutState.Failed("You need to be signed in to check out.")
            return
        }
        if (pendingVerification != null) {
            // A previous payment already succeeded and is waiting to be confirmed -
            // starting a new checkout here would risk charging twice for the same cart.
            _checkoutState.value = CheckoutState.PaymentSucceededPendingVerification(
                pendingVerification!!.result.paymentId
            )
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
                        verifyAndSaveOrder(uid, address.id, address, cartItems, result)
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
        address: AddressModel?,
        cartItems: List<CartItem>,
        result: RazorpayResult.Success
    ) {
        // The payment already succeeded at this point - only the confirmation
        // step can fail (e.g. backend cold start on Render's free tier).
        // Retry a few times with backoff before giving up, since a single
        // slow response shouldn't force the user to risk a duplicate charge.
        val maxAttempts = 3

        for (attempt in 1..maxAttempts) {
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
                    // Signature genuinely didn't match - retrying won't help, stop immediately.
                    _checkoutState.value = CheckoutState.Failed(
                        verifyResponse.message ?: "Payment could not be verified"
                    )
                    return
                }

                saveLocalOrderMirrorAndClearCart(uid, addressId, address, cartItems, result, verifyResponse.orderId)
                pendingVerification = null
                _checkoutState.value = CheckoutState.Success(verifyResponse.orderId ?: result.orderId)
                return
            } catch (e: Exception) {
                if (attempt < maxAttempts) {
                    kotlinx.coroutines.delay(attempt * 2000L) // 2s, then 4s
                }
            }
        }

        // All retries exhausted but the payment itself succeeded - remember
        // enough to retry verification later without ever charging again.
        pendingVerification = PendingVerification(uid, addressId, address, cartItems, result)
        _checkoutState.value = CheckoutState.PaymentSucceededPendingVerification(result.paymentId)
    }

    private suspend fun saveLocalOrderMirrorAndClearCart(
        uid: String,
        addressId: String,
        address: AddressModel?,
        cartItems: List<CartItem>,
        result: RazorpayResult.Success,
        backendOrderId: String?
    ) {
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
        val totalAmount = cartItems.sumOf {
            (it.product.actualPrice.toDoubleOrNull() ?: 0.0) * it.quantity
        }
        val orderId = backendOrderId ?: result.orderId
        val userOrderRef = firestore.collection("users").document(uid)
            .collection("orders").document(orderId)

        val addressSnapshot = address?.let {
            mapOf(
                "name" to it.name,
                "addressLine" to it.addressLine,
                "city" to it.city,
                "state" to it.state,
                "pincode" to it.pincode
            )
        }

        userOrderRef.set(
            mapOf(
                "id" to orderId,
                "items" to orderItems,
                "addressId" to addressId,
                "address" to addressSnapshot,
                "totalAmount" to totalAmount,
                "razorpayOrderId" to result.orderId,
                "razorpayPaymentId" to result.paymentId,
                "status" to "placed",
                "createdAt" to System.currentTimeMillis()
            )
        ).await()

        firestore.collection("users").document(uid)
            .update("cartItems", emptyMap<String, Long>())
            .await()
    }

    /**
     * Safe to call as many times as needed - only re-runs /verify (which is
     * now idempotent on the backend), never /create-order, so it can never
     * cause a second charge.
     */
    fun retryVerification() {
        val pending = pendingVerification ?: return
        viewModelScope.launch {
            _checkoutState.value = CheckoutState.VerifyingPayment
            verifyAndSaveOrder(pending.uid, pending.addressId, pending.address, pending.cartItems, pending.result)
        }
    }

    fun resetCheckoutState() {
        _checkoutState.value = CheckoutState.Idle
    }
}
