package com.project.veggiekart.model

data class UserModel(
    val uid: String = "",
    val phone: String = "",
    val createdAt: Long = 0L,
    val cartItems: Map<String, Long> = emptyMap(),
)
