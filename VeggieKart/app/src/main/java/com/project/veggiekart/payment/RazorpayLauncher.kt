package com.project.veggiekart.payment

import android.app.Activity
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed class RazorpayResult {
    data class Success(
        val paymentId: String,
        val orderId: String,
        val signature: String
    ) : RazorpayResult()

    data class Failure(val code: Int, val description: String) : RazorpayResult()
}

/**
 * Razorpay's Checkout SDK reports results through an Activity-level listener
 * (the hosting Activity must implement PaymentResultWithDataListener and
 * forward onPaymentSuccess/onPaymentError into resultCallback). This wraps
 * that callback shape into a suspend function so it can be awaited from a
 * ViewModel/coroutine instead of managing listener state by hand.
 */
object RazorpayLauncher {

    var resultCallback: ((RazorpayResult) -> Unit)? = null

    fun open(
        activity: Activity,
        keyId: String,
        razorpayOrderId: String,
        amountInPaise: Long,
        userName: String,
        userEmail: String,
        userPhone: String
    ) {
        val checkout = Checkout()
        checkout.setKeyID(keyId)

        val options = JSONObject().apply {
            put("name", "VeggieKart")
            put("description", "Order payment")
            put("order_id", razorpayOrderId)
            put("amount", amountInPaise)
            put("currency", "INR")
            put("prefill", JSONObject().apply {
                put("email", userEmail)
                put("contact", userPhone)
                put("name", userName)
            })
        }

        checkout.open(activity, options)
    }

    suspend fun awaitResult(): RazorpayResult = suspendCancellableCoroutine { continuation ->
        resultCallback = { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        continuation.invokeOnCancellation { resultCallback = null }
    }
}
