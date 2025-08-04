package com.project.veggiekart.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.project.veggiekart.pages.CategoriesPage
import com.project.veggiekart.pages.HomePage
import com.project.veggiekart.pages.ReorderPage

@Composable
fun HomeScreen(modifier: Modifier = Modifier, navcontroller: NavHostController) {
    val navItemList = listOf(
        NavItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem("Categories", Icons.Filled.Widgets, Icons.Outlined.Widgets),
        NavItem("Cart", Icons.Filled.ShoppingBasket, Icons.Outlined.ShoppingBasket),
    )

    var selectedIndex by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItemList.forEachIndexed { index, navItem ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = index
                        },
                        label = {
                            Text(navItem.label)
                        },
                        icon = {
                            Icon(
                                imageVector = if(selectedIndex == index) navItem.selectedIcon else navItem.unSelectedIcon,
                                contentDescription = navItem.label
                            )
                        }
                    )
                }
            }
        }
    ) {
        ContentScreen(modifier = modifier.padding(it), selectedIndex)
    }

}

@Composable
fun ContentScreen(modifier: Modifier = Modifier, selectedIndex: Int) {
    when (selectedIndex) {
        0 -> HomePage(modifier)
        1 -> CategoriesPage(modifier)
        2 -> ReorderPage(modifier)
    }
}

data class NavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unSelectedIcon: ImageVector
)

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(navcontroller = NavHostController(LocalContext.current))
}