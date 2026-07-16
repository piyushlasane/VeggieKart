package com.project.veggiekart.model

data class OrderItemModel(
    val productId: String = "",
    val title: String = "",
    val price: String = "",
    val quantity: Long = 0L
)

data class OrderModel(
    val id: String = "",
    val userId: String = "",
    val items: List<OrderItemModel> = emptyList(),
    val addressId: String = "",
    val totalAmount: Double = 0.0,
    val razorpayOrderId: String = "",
    val razorpayPaymentId: String = "",
    // "placed" is the only status the app writes locally, the payment
    // backend independently writes the verified copy to Firestore's
    // top-level "orders" collection after signature verification.
    val status: String = "placed",
    val createdAt: Long = System.currentTimeMillis()
)
