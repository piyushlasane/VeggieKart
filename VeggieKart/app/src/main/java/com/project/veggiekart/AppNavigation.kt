package com.project.veggiekart

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.project.veggiekart.screens.AuthScreen
import com.project.veggiekart.screens.LoginScreen
import com.project.veggiekart.screens.HomeScreen

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth"){
            AuthScreen(modifier, navController)
        }
        composable("login"){
            LoginScreen(modifier, navController)
        }
        composable("homescreen"){
            HomeScreen(modifier, navController)
        }
    }
}