package com.project.veggiekart.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.project.veggiekart.AppUtil
import com.project.veggiekart.GlobalNavigation
import com.project.veggiekart.model.ProductModel
import com.project.veggiekart.viewmodel.CartViewModel

@Composable
fun ProductItemView(
    modifier: Modifier = Modifier,
    product: ProductModel,
    cartViewModel: CartViewModel = viewModel(),
    snackbarHostState: SnackbarHostState? = null
) {
    var isAddingToCart by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    val cartState by cartViewModel.cartState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Check if product is in cart
    val isInCart = cartState.items.any { it.product.id == product.id }

    Card(
        modifier = modifier
            .padding(8.dp)
            .clickable {
                GlobalNavigation.navController.navigate("product-details/" + product.id)
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            AsyncImage(
                model = product.images.firstOrNull(),
                contentDescription = product.title,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .align(Alignment.CenterHorizontally)
                    .background(color = MaterialTheme.colorScheme.surfaceContainer)
            )
            Text(
                product.title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹" + product.price,
                    fontSize = 14.sp,
                    style = TextStyle(textDecoration = TextDecoration.LineThrough)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "₹" + product.actualPrice,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        if (!isLoggedIn) {
                            snackbarHostState?.let {
                                AppUtil.showSnackbar(
                                    scope,
                                    it,
                                    "Please login to add items to cart"
                                )
                            }
                            GlobalNavigation.navController.navigate("login")
                            return@IconButton
                        }

                        isAddingToCart = true

                        if (isInCart) {
                            // Remove from cart if already in cart
                            cartViewModel.removeFromCart(product.id) { success, message ->
                                isAddingToCart = false
                                snackbarHostState?.let {
                                    AppUtil.showSnackbar(scope, it, message)
                                }
                            }
                        } else {
                            // Add to cart if not in cart
                            cartViewModel.addToCart(product.id) { success, message ->
                                isAddingToCart = false
                                snackbarHostState?.let {
                                    AppUtil.showSnackbar(scope, it, message)
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(30.dp),
                    enabled = !isAddingToCart
                ) {
                    if (isAddingToCart) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            modifier = Modifier.size(21.dp),
                            imageVector = if (isInCart) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                            contentDescription = if (isInCart) "Remove from Cart" else "Add to Cart",
                            tint = if (isInCart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}