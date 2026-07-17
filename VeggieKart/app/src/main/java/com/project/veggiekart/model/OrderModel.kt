package com.project.veggiekart.model

data class OrderItemModel(
    val productId: String = "",
    val title: String = "",
    val price: String = "",
    val quantity: Long = 0L
)

/**
 * A snapshot of the delivery address AT THE TIME the order was placed.
 * Deliberately not just an addressId reference - if the user edits or
 * deletes that address later, past orders must still show what was
 * actually delivered where.
 */
data class OrderAddressSnapshot(
    val name: String = "",
    val addressLine: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = ""
)

data class OrderModel(
    val id: String = "",
    val userId: String = "",
    val items: List<OrderItemModel> = emptyList(),
    val addressId: String = "",
    val address: OrderAddressSnapshot? = null,
    val totalAmount: Double = 0.0,
    val razorpayOrderId: String = "",
    val razorpayPaymentId: String = "",
    // "placed" is the only status the app writes locally, the payment
    // backend independently writes the verified copy to Firestore's
    // top-level "orders" collection after signature verification.
    val status: String = "placed",
    val createdAt: Long = System.currentTimeMillis()
)
