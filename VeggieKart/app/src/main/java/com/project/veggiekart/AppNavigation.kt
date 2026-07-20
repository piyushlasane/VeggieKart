package com.project.veggiekart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.project.veggiekart.model.UserModel
import com.project.veggiekart.pages.*
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

    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController = navController, startDestination = startDestination!!) {
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
        composable("add-address/{addressId}") {
            val addressId = it.arguments?.getString("addressId")
            val actualAddressId = if (addressId == "null") null else addressId
            AddAddressScreen(modifier, navController, actualAddressId)
        }
        composable("category-products/{categoryId}") {
            val categoryId = it.arguments?.getString("categoryId")
            CategoryProductsPage(modifier, categoryId ?: "")
        }
        composable("search") {
            SearchPage(modifier, navController)
        }
        composable("product-details/{productId}") {
            val productId = it.arguments?.getString("productId")
            ProductDetailsPage(modifier, productId ?: "")
        }
        composable("checkout") {
            CheckoutScreen(modifier, navController)
        }
        composable("orders") {
            OrdersPage(modifier)
        }
        composable("order-confirmation/{orderId}") {
            val orderId = it.arguments?.getString("orderId")
            OrderConfirmationScreen(modifier, navController, orderId ?: "")
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