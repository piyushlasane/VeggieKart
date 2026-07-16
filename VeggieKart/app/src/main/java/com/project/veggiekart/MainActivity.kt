package com.project.veggiekart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.project.veggiekart.payment.RazorpayLauncher
import com.project.veggiekart.payment.RazorpayResult
import com.project.veggiekart.ui.theme.VeggieKartTheme
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

// minSdk is 24; nothing here actually needs API 33, so no @RequiresApi.
// PaymentResultWithDataListener is how Razorpay's Checkout SDK reports the
// outcome back - it can only call back into an Activity, so MainActivity
// implements it and forwards the result into RazorpayLauncher's coroutine.
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeggieKartTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String, paymentData: PaymentData) {
        val orderId = paymentData.orderId
        val signature = paymentData.signature
        if (orderId != null && signature != null) {
            RazorpayLauncher.resultCallback?.invoke(
                RazorpayResult.Success(razorpayPaymentId, orderId, signature)
            )
        } else {
            RazorpayLauncher.resultCallback?.invoke(
                RazorpayResult.Failure(-1, "Missing order id or signature in payment result")
            )
        }
    }

    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        RazorpayLauncher.resultCallback?.invoke(
            RazorpayResult.Failure(code, description ?: "Payment failed")
        )
    }
}
