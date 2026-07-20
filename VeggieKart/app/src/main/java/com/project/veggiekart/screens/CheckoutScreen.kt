package com.project.veggiekart.screens

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.veggiekart.AppUtil
import com.project.veggiekart.model.AddressModel
import com.project.veggiekart.model.UserModel
import com.project.veggiekart.viewmodel.CartViewModel
import com.project.veggiekart.viewmodel.CheckoutState
import com.project.veggiekart.viewmodel.OrderViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    cartViewModel: CartViewModel = viewModel(LocalContext.current as ComponentActivity),
    orderViewModel: OrderViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val cartState by cartViewModel.cartState.collectAsState()
    val checkoutState by orderViewModel.checkoutState.collectAsState()

    var addresses by remember { mutableStateOf<List<AddressModel>>(emptyList()) }
    var selectedAddressId by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }
    var isLoadingAddresses by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            try {
                val doc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                val user = doc.toObject(UserModel::class.java)
                addresses = user?.addresses ?: emptyList()
                selectedAddressId = addresses.find { it.isDefault }?.id ?: addresses.firstOrNull()?.id
                userName = user?.name ?: ""
                userPhone = user?.phone ?: ""
            } catch (e: Exception) {
                AppUtil.showSnackbar(scope, snackbarHostState, "Failed to load addresses")
            } finally {
                isLoadingAddresses = false
            }
        }
    }

    // Navigate away once payment is confirmed and the order is saved.
    LaunchedEffect(checkoutState) {
        val state = checkoutState
        if (state is CheckoutState.Success) {
            navController.navigate("order-confirmation/${state.orderId}") {
                popUpTo("home")
            }
        } else if (state is CheckoutState.Failed) {
            AppUtil.showSnackbar(scope, snackbarHostState, state.message)
        }
        // PaymentSucceededPendingVerification is handled inline below, not as a snackbar -
        // it needs to stay visible and offer a retry action, not disappear after a few seconds.
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = modifier.fillMaxSize().padding(innerPadding)) {
            when {
                isLoadingAddresses -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                addresses.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Add a delivery address before checking out", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { navController.navigate("add-address/null") }) {
                            Text("Add Address")
                        }
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                            item {
                                Text("Deliver to", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                            }
                            items(addresses, key = { it.id }) { address ->
                                AddressSelectRow(
                                    address = address,
                                    selected = address.id == selectedAddressId,
                                    onSelect = { selectedAddressId = address.id }
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            item {
                                Spacer(Modifier.height(16.dp))
                                Text("Order Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                            }
                            items(cartState.items, key = { it.product.id }) { cartItem ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${cartItem.product.title} x${cartItem.quantity}",
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val lineTotal = (cartItem.product.actualPrice.toDoubleOrNull() ?: 0.0) * cartItem.quantity
                                    Text("₹${"%.2f".format(lineTotal)}")
                                }
                            }
                        }

                        CheckoutBottomBar(
                            totalAmount = cartState.totalAmount,
                            isProcessing = checkoutState is CheckoutState.CreatingOrder ||
                                    checkoutState is CheckoutState.AwaitingPayment ||
                                    checkoutState is CheckoutState.VerifyingPayment,
                            pendingPaymentId = (checkoutState as? CheckoutState.PaymentSucceededPendingVerification)?.paymentId,
                            onPayNow = {
                                val selectedAddress = addresses.find { it.id == selectedAddressId }
                                if (activity == null || selectedAddress == null) return@CheckoutBottomBar
                                orderViewModel.startCheckout(
                                    activity = activity,
                                    cartItems = cartState.items,
                                    totalAmount = cartState.totalAmount,
                                    address = selectedAddress,
                                    userName = userName,
                                    userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "",
                                    userPhone = userPhone
                                )
                            },
                            onRetryVerification = { orderViewModel.retryVerification() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressSelectRow(address: AddressModel, selected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(Modifier.padding(start = 8.dp)) {
                Text(address.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${address.addressLine}, ${address.city}, ${address.state} - ${address.pincode}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CheckoutBottomBar(
    totalAmount: Double,
    isProcessing: Boolean,
    pendingPaymentId: String?,
    onPayNow: () -> Unit,
    onRetryVerification: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Payable", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "₹${"%.2f".format(totalAmount)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))

            if (pendingPaymentId != null) {
                // Payment already succeeded - never show "Pay Now" here, it would charge again.
                // Only a verification retry (safe, idempotent on the backend) is allowed.
                Text(
                    "Your payment went through (id: $pendingPaymentId) but we couldn't confirm it yet. Don't pay again - just retry confirming below.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onRetryVerification,
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Retry Confirmation", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Button(
                    onClick = onPayNow,
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Pay Now", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
