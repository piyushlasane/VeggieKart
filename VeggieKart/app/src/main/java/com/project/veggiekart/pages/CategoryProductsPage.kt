package com.project.veggiekart.pages

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.project.veggiekart.components.ProductItemView
import com.project.veggiekart.model.ProductModel
import com.project.veggiekart.viewmodel.CartViewModel

@Composable
fun CategoryProductsPage(
    modifier: Modifier = Modifier,
    categoryId: String,
) {
    val productsList = remember {
        mutableStateOf<List<ProductModel>>(emptyList())
    }
    val snackbarHostState = remember { SnackbarHostState() }
    // Activity-scoped so this resolves to the SAME CartViewModel instance as every
    // other screen - already loaded, no reload needed here.
    val cartViewModel: CartViewModel = viewModel(LocalContext.current as ComponentActivity)

    LaunchedEffect(categoryId) {
        // Load products for this category
        Firebase.firestore.collection("data").document("stock")
            .collection("products")
            .whereEqualTo("category", categoryId)
            .get().addOnCompleteListener {
                if (it.isSuccessful) {
                    val resultList = it.result.documents.mapNotNull { doc ->
                        doc.toObject(ProductModel::class.java)
                    }
                    productsList.value = resultList
                }
            }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(
            items = productsList.value.chunked(2),
            key = { rowItems -> rowItems.joinToString("_") { it.id } }
        ) { rowItems ->
            Row {
                rowItems.forEach {
                    ProductItemView(
                        product = it,
                        modifier = Modifier.weight(1f),
                        cartViewModel = cartViewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}