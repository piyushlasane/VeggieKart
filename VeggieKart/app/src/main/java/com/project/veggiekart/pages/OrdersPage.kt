package com.project.veggiekart.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.veggiekart.model.OrderModel
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed class OrdersUiState {
    data object Loading : OrdersUiState()
    data object NotSignedIn : OrdersUiState()
    data class Error(val message: String) : OrdersUiState()
    data class Loaded(val orders: List<OrderModel>) : OrdersUiState()
}

@Composable
fun OrdersPage(modifier: Modifier = Modifier) {
    var uiState by remember { mutableStateOf<OrdersUiState>(OrdersUiState.Loading) }
    // Bumping this re-triggers the LaunchedEffect below, used for the retry button.
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            uiState = OrdersUiState.NotSignedIn
            return@LaunchedEffect
        }

        uiState = OrdersUiState.Loading
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val orders = snapshot.documents.mapNotNull { it.toObject(OrderModel::class.java) }
            uiState = OrdersUiState.Loaded(orders)
        } catch (e: Exception) {
            uiState = OrdersUiState.Error(e.message ?: "Something went wrong while loading your orders")
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "My Orders",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is OrdersUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is OrdersUiState.NotSignedIn -> {
                    EmptyState(
                        title = "Sign in to see your orders",
                        subtitle = "Your order history will show up here once you're signed in."
                    )
                }

                is OrdersUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Couldn't load your orders",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            state.message,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { reloadKey++ }) {
                            Text("Retry")
                        }
                    }
                }

                is OrdersUiState.Loaded -> {
                    if (state.orders.isEmpty()) {
                        EmptyState(
                            title = "No orders yet",
                            subtitle = "When you place an order, it'll show up here."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(state.orders, key = { it.id }) { order ->
                                OrderCard(order)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OrderCard(order: OrderModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Order #${order.id.take(8)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        formatOrderDate(order.createdAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(order.status)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${item.title} x${item.quantity}",
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    val lineTotal = (item.price.toDoubleOrNull() ?: 0.0) * item.quantity
                    Text("₹${"%.2f".format(lineTotal)}", fontSize = 13.sp)
                }
            }

            // Older orders placed before the address-snapshot fix may not have this -
            // fall back gracefully instead of showing a blank/broken section.
            val address = order.address
            if (address != null && address.addressLine.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Delivered to: ${address.name}, ${address.addressLine}, ${address.city} - ${address.pincode}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (order.addressId.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Delivery address unavailable (may have been edited or removed since this order)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                // Fall back to summing items for any pre-existing orders saved
                // before totalAmount was written explicitly.
                val displayTotal = if (order.totalAmount > 0.0) {
                    order.totalAmount
                } else {
                    order.items.sumOf { (it.price.toDoubleOrNull() ?: 0.0) * it.quantity }
                }
                Text(
                    "₹${"%.2f".format(displayTotal)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, label) = when (status.lowercase()) {
        "placed" -> MaterialTheme.colorScheme.primaryContainer to "Placed"
        "delivered" -> MaterialTheme.colorScheme.primaryContainer to "Delivered"
        "cancelled" -> MaterialTheme.colorScheme.errorContainer to "Cancelled"
        else -> MaterialTheme.colorScheme.surfaceVariant to status.replaceFirstChar { it.uppercase() }
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun formatOrderDate(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return ""
    val formatter = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())
    return formatter.format(Date(timestampMillis))
}