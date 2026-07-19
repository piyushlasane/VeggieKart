package com.project.veggiekart.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.project.veggiekart.pages.HomePage
import com.project.veggiekart.pages.CartPage
import com.project.veggiekart.pages.OrdersPage
import com.project.veggiekart.viewmodel.CartViewModel

@Composable
fun HomeScreen(modifier: Modifier = Modifier, navcontroller: NavHostController) {
    val navItemList = listOf(
        NavItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem("Cart", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
        NavItem("Orders", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag),
    )

    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Activity-scoped: this is now the ONE CartViewModel instance shared by every
    // screen in the app (Home, Cart, product details, category grid, checkout).
    val cartViewModel: CartViewModel = viewModel(LocalContext.current as ComponentActivity)
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

    // Single place the cart gets (re)loaded from Firestore: once when HomeScreen is
    // first entered, and again if the login state actually changes (e.g. logout -> login).
    // No other screen should call loadCart() after a mutation - they update state locally.
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            cartViewModel.loadCart()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                navItemList.forEachIndexed { index, navItem ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        label = { Text(navItem.label) },
                        icon = {
                            Icon(
                                imageVector = if (selectedIndex == index) navItem.selectedIcon else navItem.unSelectedIcon,
                                contentDescription = navItem.label
                            )
                        })
                }
            }
        }) {
        val bottomPadding = it.calculateBottomPadding().coerceAtMost(40.dp)
        ContentScreen(
            modifier = modifier.padding(bottom = bottomPadding),
            selectedIndex = selectedIndex,
            snackbarHostState = snackbarHostState,
            cartViewModel = cartViewModel
        )
    }

}

@Composable
fun ContentScreen(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    snackbarHostState: SnackbarHostState,
    cartViewModel: CartViewModel
) {
    when (selectedIndex) {
        0 -> HomePage(modifier, snackbarHostState)
        1 -> CartPage(modifier, cartViewModel, snackbarHostState)
        2 -> OrdersPage(modifier)
    }
}

data class NavItem(
    val label: String, val selectedIcon: ImageVector, val unSelectedIcon: ImageVector
)