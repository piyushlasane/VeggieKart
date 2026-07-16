package com.project.veggiekart.network

import retrofit2.http.Body
import retrofit2.http.POST

data class CreateOrderRequest(
    val amountInRupees: Double,
    val userId: String
)

data class CreateOrderResponse(
    val razorpayOrderId: String,
    val razorpayKeyId: String,
    val amountInPaise: Long,
    val currency: String
)

data class OrderItemDto(
    val productId: String,
    val name: String,
    val quantity: Int,
    val price: Double
)

data class VerifyPaymentRequest(
    val razorpayOrderId: String,
    val razorpayPaymentId: String,
    val razorpaySignature: String,
    val userId: String,
    val addressId: String,
    val items: List<OrderItemDto>
)

data class VerifyPaymentResponse(
    val verified: Boolean,
    val orderId: String? = null,
    val message: String? = null
)

interface PaymentApi {
    @POST("api/payment/create-order")
    suspend fun createOrder(@Body request: CreateOrderRequest): CreateOrderResponse

    @POST("api/payment/verify")
    suspend fun verifyPayment(@Body request: VerifyPaymentRequest): VerifyPaymentResponse
}
