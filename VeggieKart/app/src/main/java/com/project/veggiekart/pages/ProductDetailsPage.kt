package com.project.veggiekart.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.project.veggiekart.AppUtil
import com.project.veggiekart.GlobalNavigation
import com.project.veggiekart.model.ProductModel
import com.project.veggiekart.viewmodel.CartViewModel
import com.tbuonomo.viewpagerdotsindicator.compose.DotsIndicator
import com.tbuonomo.viewpagerdotsindicator.compose.model.DotGraphic
import com.tbuonomo.viewpagerdotsindicator.compose.type.ShiftIndicatorType

@Composable
fun ProductDetailsPage(
    modifier: Modifier = Modifier,
    productId: String,
    cartViewModel: CartViewModel = viewModel()
) {
    var product by remember { mutableStateOf(ProductModel()) }
    var isAddingToCart by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    val cartState by cartViewModel.cartState.collectAsState()

    // Get quantity from cart
    val quantityInCart = cartState.items.find { it.product.id == productId }?.quantity ?: 0L
    val isInCart = quantityInCart > 0

    LaunchedEffect(key1 = Unit) {
        // Load cart to ensure fresh data
        if (isLoggedIn) {
            cartViewModel.loadCart()
        }

        // Load product details
        Firebase.firestore.collection("data").document("stock").collection("products")
            .document(productId).get().addOnCompleteListener {
                if (it.isSuccessful) {
                    val result = it.result.toObject(ProductModel::class.java)
                    if (result != null) {
                        product = result
                    }
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = product.title,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(8.dp)
        )

        Spacer(modifier = Modifier.padding(8.dp))

        Column {
            val pagerState = rememberPagerState(0) {
                product.images.size
            }
            HorizontalPager(state = pagerState, pageSpacing = 24.dp) {
                AsyncImage(
                    model = product.images.get(it),
                    contentDescription = "Product Images",
                    contentScale = ContentScale.FillBounds,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .align(Alignment.CenterHorizontally)
                        .background(color = MaterialTheme.colorScheme.surfaceContainer)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DotsIndicator(
                dotCount = product.images.size, type = ShiftIndicatorType(
                    DotGraphic(
                        color = MaterialTheme.colorScheme.primary, size = 5.dp
                    )
                ), pagerState = pagerState
            )
        }

        Spacer(modifier = Modifier.padding(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "₹" + product.price,
                fontSize = 16.sp,
                style = TextStyle(textDecoration = TextDecoration.LineThrough)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "₹" + product.actualPrice, fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                AppUtil.showSnackbar(scope, snackbarHostState, "Wishlist coming soon!")
            }) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = "Add to Favorites"
                )
            }
        }

        Spacer(modifier = Modifier.padding(8.dp))

        // Cart Controls - Show different UI based on cart status
        if (isInCart) {
            // Product is in cart - Show quantity controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Remove/Delete button
                OutlinedButton(
                    onClick = {
                        isUpdating = true
                        cartViewModel.removeFromCart(productId) { success, message ->
                            isUpdating = false
                            AppUtil.showSnackbar(scope, snackbarHostState, message)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = !isUpdating,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Remove from cart",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // Quantity controls
                Row(
                    modifier = Modifier
                        .weight(1.5f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isUpdating = true
                            cartViewModel.updateQuantity(
                                productId,
                                quantityInCart - 1
                            ) { success, message ->
                                isUpdating = false
                                if (!success) {
                                    AppUtil.showSnackbar(scope, snackbarHostState, message)
                                }
                            }
                        },
                        enabled = !isUpdating
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = if (isUpdating) "..." else quantityInCart.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    IconButton(
                        onClick = {
                            isUpdating = true
                            cartViewModel.updateQuantity(
                                productId,
                                quantityInCart + 1
                            ) { success, message ->
                                isUpdating = false
                                if (!success) {
                                    AppUtil.showSnackbar(scope, snackbarHostState, message)
                                }
                            }
                        },
                        enabled = !isUpdating
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        } else {
            // Product not in cart - Show Add to Cart button
            Button(
                onClick = {
                    if (!isLoggedIn) {
                        AppUtil.showSnackbar(
                            scope,
                            snackbarHostState,
                            "Please login to add items to cart"
                        )
                        GlobalNavigation.navController.navigate("login")
                        return@Button
                    }

                    isAddingToCart = true
                    cartViewModel.addToCart(product.id) { success, message ->
                        isAddingToCart = false
                        AppUtil.showSnackbar(scope, snackbarHostState, message)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isAddingToCart
            ) {
                if (isAddingToCart) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = "Add to Cart", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.padding(16.dp))

        Text(
            text = "Product Description: ", fontSize = 18.sp, fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.padding(8.dp))

        Text(
            text = product.description, fontSize = 16.sp
        )
    }
}