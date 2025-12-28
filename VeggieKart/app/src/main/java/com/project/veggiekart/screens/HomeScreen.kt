package com.project.veggiekart.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.project.veggiekart.pages.HomePage
import com.project.veggiekart.pages.CartPage

@Composable
fun HomeScreen(modifier: Modifier = Modifier, navcontroller: NavHostController) {
    val navItemList = listOf(
        NavItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
        // NavItem("Categories", Icons.Filled.Widgets, Icons.Outlined.Widgets),
        NavItem("Cart", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    )

    var selectedIndex by rememberSaveable { mutableStateOf(0) }

    Scaffold(
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
        val bottomPadding = it.calculateBottomPadding().coerceAtMost(80.dp)
        ContentScreen(modifier = modifier.padding(bottom = bottomPadding), selectedIndex)
    }

}

@Composable
fun ContentScreen(modifier: Modifier = Modifier, selectedIndex: Int) {
    when (selectedIndex) {
        0 -> HomePage(modifier)
        1 -> CartPage(modifier)
        // 1 -> CategoriesPage(modifier)
    }
}

data class NavItem(
    val label: String, val selectedIcon: ImageVector, val unSelectedIcon: ImageVector
)
