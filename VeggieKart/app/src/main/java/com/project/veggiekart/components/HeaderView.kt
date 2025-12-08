package com.project.veggiekart.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.project.veggiekart.AddressUpdateNotifier
import com.project.veggiekart.GlobalNavigation
import com.project.veggiekart.model.AddressModel
import com.project.veggiekart.model.UserModel
import kotlinx.coroutines.tasks.await

@Composable
fun HeaderView(modifier: Modifier = Modifier) {
    var defaultAddress by remember { mutableStateOf<AddressModel?>(null) }
    var showAddressSheet by remember { mutableStateOf(false) }
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    val addressUpdateTrigger by AddressUpdateNotifier.updateTrigger

    // Load default address
    LaunchedEffect(addressUpdateTrigger, isLoggedIn) {
        Log.d("ADDR", "Header LaunchedEffect trigger=$addressUpdateTrigger isLoggedIn=$isLoggedIn")
        if (isLoggedIn) {
            defaultAddress = loadDefaultAddress()
            Log.d("ADDR", "loadDefaultAddress -> ${defaultAddress?.id}:${defaultAddress?.isDefault}")

        } else {
            defaultAddress = null
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (isLoggedIn) {
                            showAddressSheet = true
                        } else {
                            GlobalNavigation.navController.navigate("login")
                        }
                    }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Select Location",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Deliver to ", fontSize = 16.sp)
                    Text(
                        text = defaultAddress?.city ?: "Select Address",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Location",
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(text = defaultAddress?.let { "${it.addressLine}, ${it.pincode}" }
                    ?: if (isLoggedIn) "Add your delivery address" else "Login to add address",
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = {
                GlobalNavigation.navController.navigate("profile")
            }) {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = "Your Account or Profile"
                )
            }
        }
    }

    // Address Selection Bottom Sheet
    if (showAddressSheet) {
        AddressSelectionBottomSheet(
            onDismiss = { showAddressSheet = false },
            onAddressSelected = { address -> defaultAddress = address }
        )
    }

}

suspend fun loadDefaultAddress(): AddressModel? {
    try {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("ADDR", "loadDefaultAddress: uid=$uid")
        if (uid == null) return null

        // Try server first to avoid cache surprises
        val docSnap = try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get(Source.SERVER)
                .await()
        } catch (e: Exception) {
            Log.d("ADDR", "server read failed: ${e.message}. trying default source")
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .await()
        }

        Log.d("ADDR", "doc.exists=${docSnap.exists()}")
        val raw = docSnap.data
        Log.d("ADDR", "doc.data = $raw")

        val rawAddresses = docSnap.get("addresses")
        Log.d("ADDR", "raw addresses field = $rawAddresses (type=${rawAddresses?.javaClass})")

        if (rawAddresses is List<*>) {
            rawAddresses.forEachIndexed { i, item ->
                Log.d("ADDR", "address[$i] raw = $item (type=${item?.javaClass})")
            }
        }

        // Try normal deserialization first
        val user = try {
            docSnap.toObject(UserModel::class.java)
        } catch (e: Exception) {
            Log.d("ADDR", "deserialization to UserModel failed: ${e.message}")
            null
        }
        Log.d("ADDR", "deserialized user = $user")
        val defaultFromModel = user?.addresses?.find { it.isDefault }
        Log.d("ADDR", "deserialized default = ${defaultFromModel?.id}:${defaultFromModel?.isDefault}")

        if (defaultFromModel != null) return defaultFromModel.copy()

        // Fallback: manual parse of rawAddresses (handles Map<String, Any> entries)
        if (rawAddresses is List<*>) {
            rawAddresses.forEach { item ->
                if (item is Map<*, *>) {
                    // Common key variants
                    val id = item["id"] ?: item["ID"] ?: item["addressId"] ?: item["address_id"]
                    val name = item["name"] ?: item["fullName"]
                    val phone = item["phone"] ?: item["mobile"] ?: item["contact"]
                    val addressLine = item["addressLine"] ?: item["address_line"] ?: item["address"]
                    val city = item["city"]
                    val state = item["state"]
                    val pincode = item["pincode"] ?: item["pin"] ?: item["zip"]
                    val addressType = item["addressType"] ?: item["type"]
                    val isDefRaw = item["isDefault"] ?: item["is_default"] ?: item["default"]

                    // Normalize isDefault
                    val isDefault = when (isDefRaw) {
                        is Boolean -> isDefRaw
                        is String -> isDefRaw.equals("true", true) || isDefRaw == "1"
                        is Number -> isDefRaw.toInt() != 0
                        else -> false
                    }

                    Log.d("ADDR", "manual parse: id=$id isDefaultRaw=$isDefRaw normalized=$isDefault")

                    if (isDefault) {
                        // Build AddressModel robustly — adapt to your data class constructor
                        val built = AddressModel(
                            id = id?.toString() ?: "",
                            name = name?.toString() ?: "",
                            phone = phone?.toString() ?: "",
                            addressLine = addressLine?.toString() ?: "",
                            city = city?.toString() ?: "",
                            state = state?.toString() ?: "",
                            pincode = pincode?.toString() ?: "",
                            addressType = addressType?.toString() ?: "",
                            isDefault = true
                        )
                        Log.d("ADDR", "manual default built = $built")
                        return built
                    }
                }
            }
        }

        Log.d("ADDR", "no default found in doc (after manual parse)")
        return null
    } catch (e: Exception) {
        Log.d("ADDR", "loadDefaultAddress exception: ${e.message}")
        return null
    }
}