package com.project.veggiekart.model

import com.google.firebase.firestore.PropertyName

data class AddressModel(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val addressLine: String = "",
    val landmark: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val addressType: String = "",
    @get:PropertyName("default")
    @set:PropertyName("default")
    var isDefault: Boolean = false
)