package com.project.veggiekart

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.project.veggiekart.pages.CategoryProductsPage
import com.project.veggiekart.screens.AuthScreen
import com.project.veggiekart.screens.LoginScreen
import com.project.veggiekart.screens.HomeScreen
import com.project.veggiekart.screens.ProfileScreen

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {

    val navController = rememberNavController()
    GloabalNavigation.navController = navController

    val isLoggedIn = Firebase.auth.currentUser != null
    val firstPage = if (isLoggedIn) "home" else "auth"

    NavHost(navController = navController, startDestination = firstPage) {
        composable("auth"){
            AuthScreen(modifier, navController)
        }
        composable("login"){
            LoginScreen(modifier, navController)
        }
        composable("home"){
            HomeScreen(modifier, navController)
        }
        composable("profile"){
            ProfileScreen(modifier, navController)
        }
        composable("category-products/{categoryId}"){
            val categoryId = it.arguments?.getString("categoryId")
            CategoryProductsPage(modifier, categoryId?: "")
        }
    }
}

object GloabalNavigation{
    lateinit var navController: NavHostController
}