package com.project.veggiekart.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    cartViewModel: CartViewModel = viewModel(LocalContext.current as ComponentActivity),
    snackbarHostState: SnackbarHostState? = null
) {
    val scope = rememberCoroutineScope()
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    val cartState by cartViewModel.cartState.collectAsState()

    // Check if product is in cart - reads straight from the shared cart state,
    // no local "isAddingToCart" loading flag needed since updates are optimistic/instant now.
    val isInCart = cartState.items.any { it.product.id == product.id }

    fun onCartIconClick() {
        if (!isLoggedIn) {
            snackbarHostState?.let {
                AppUtil.showSnackbar(scope, it, "Please login to add items to cart")
            }
            GlobalNavigation.navController.navigate("login")
            return
        }

        if (isInCart) {
            cartViewModel.removeFromCart(product.id) { success, message ->
                snackbarHostState?.let { AppUtil.showSnackbar(scope, it, message) }
            }
        } else {
            cartViewModel.addToCart(product) { success, message ->
                snackbarHostState?.let { AppUtil.showSnackbar(scope, it, message) }
            }
        }
    }

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
            // Image + floating cart button sit in a Box together so the button can
            // overlap the image's bottom-right corner, like Blinkit/Zepto/Instamart -
            // this keeps it completely off the price row, so it can never collide
            // with the price text no matter how long the title/price get.
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = product.images.firstOrNull(),
                    contentDescription = product.title,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(color = MaterialTheme.colorScheme.surfaceContainer)
                )

                Surface(
                    onClick = { onCartIconClick() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = 6.dp) // pokes slightly past the image edge
                        .size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            modifier = Modifier.size(19.dp),
                            imageVector = if (isInCart) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                            contentDescription = if (isInCart) "Remove from Cart" else "Add to Cart",
                            tint = if (isInCart) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text(
                product.title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
            )

            // Price row now only ever holds price text - nothing else competes for its space.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 2.dp),
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
            }
        }
    }
}