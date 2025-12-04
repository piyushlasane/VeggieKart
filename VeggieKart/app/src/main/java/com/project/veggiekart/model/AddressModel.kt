package com.project.veggiekart.model

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
    val addressType: String = "Home", // Home, Work, Other
    val isDefault: Boolean = false
)