package com.project.veggiekart.model

import com.google.firebase.Timestamp

data class UserModel(
    val uid: String = "",
    val phone: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val cartItems: Map<String, Long> = emptyMap(),
)
