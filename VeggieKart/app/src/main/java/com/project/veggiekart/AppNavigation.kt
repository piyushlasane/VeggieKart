package com.project.veggiekart

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.project.veggiekart.model.UserModel
import com.project.veggiekart.pages.CategoryProductsPage
import com.project.veggiekart.pages.ProductDetailsPage
import com.project.veggiekart.screens.*
import kotlinx.coroutines.tasks.await

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    GlobalNavigation.navController = navController

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val user = Firebase.auth.currentUser
        startDestination = if (user == null) {
            "auth"
        } else {
            // Check if profile is complete
            val isComplete = checkProfileComplete(user.uid)
            if (isComplete) "home" else "complete-profile"
        }
    }

    startDestination?.let { start ->
        NavHost(navController = navController, startDestination = start) {
            composable("auth") {
                AuthScreen(modifier, navController)
            }
            composable("login") {
                LoginScreen(modifier, navController)
            }
            composable("complete-profile") {
                CompleteProfileScreen(modifier, navController)
            }
            composable("home") {
                HomeScreen(modifier, navController)
            }
            composable("profile") {
                ProfileScreen(modifier, navController)
            }
            composable("edit-profile") {
                EditProfileScreen(modifier, navController)
            }
            composable("manage-addresses") {
                ManageAddressesScreen(modifier, navController)
            }
            composable("add-address") {
                AddAddressScreen(modifier, navController)
            }
            composable("category-products/{categoryId}") {
                val categoryId = it.arguments?.getString("categoryId")
                CategoryProductsPage(modifier, categoryId ?: "")
            }
            composable("product-details/{productId}") {
                val productId = it.arguments?.getString("productId")
                ProductDetailsPage(modifier, productId ?: "")
            }
        }
    }
}

suspend fun checkProfileComplete(uid: String): Boolean {
    return try {
        val doc = Firebase.firestore.collection("users").document(uid).get().await()
        val user = doc.toObject(UserModel::class.java)
        !user?.name.isNullOrEmpty() // Profile complete if name exists
    } catch (e: Exception) {
        false
    }
}

object GlobalNavigation {
    lateinit var navController: NavHostController
}