package com.project.veggiekart.components

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.veggiekart.AddressUpdateNotifier
import com.project.veggiekart.GlobalNavigation
import com.project.veggiekart.model.AddressModel
import com.project.veggiekart.model.UserModel
import kotlinx.coroutines.tasks.await

@Composable
fun HeaderView(modifier: Modifier = Modifier) {
    var defaultAddress by remember { mutableStateOf<AddressModel?>(null) }
    var showAddressSheet by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

    // Load default address
    LaunchedEffect(refreshTrigger, AddressUpdateNotifier.getUpdateTrigger()) {
        if (isLoggedIn) {
            loadDefaultAddress { address ->
                defaultAddress = address
            }
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
                    }
            ) {
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
                        text = if (defaultAddress != null) {
                            defaultAddress!!.city
                        } else  {
                            "Select Address"
                        },
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
                Text(
                    text = if (defaultAddress != null) {
                        "${defaultAddress!!.addressLine}, ${defaultAddress!!.pincode}"
                    } else if (isLoggedIn) {
                        "Add your delivery address"
                    } else {
                        "Login to add address"
                    },
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
            onDismiss = {
                showAddressSheet = false
                refreshTrigger++
            },
            onAddressSelected = { address ->
                defaultAddress = address
            }
        )
    }
}

suspend fun loadDefaultAddress(onLoaded: (AddressModel?) -> Unit) {
    try {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .await()
            val user = doc.toObject(UserModel::class.java)
            val default = user?.addresses?.find { it.isDefault }
            onLoaded(default)
        } else {
            onLoaded(null)
        }
    } catch (e: Exception) {
        onLoaded(null)
    }
}