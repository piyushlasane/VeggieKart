package com.project.veggiekart.pages

import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.project.veggiekart.components.ProductItemView
import com.project.veggiekart.model.ProductModel
import com.project.veggiekart.viewmodel.CartViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

private sealed class SearchCatalogState {
    data object Loading : SearchCatalogState()
    data class Error(val message: String) : SearchCatalogState()
    data class Loaded(val products: List<ProductModel>) : SearchCatalogState()
}

@Composable
fun SearchPage(modifier: Modifier = Modifier, navController: NavHostController) {
    var query by remember { mutableStateOf("") }
    var catalogState by remember { mutableStateOf<SearchCatalogState>(SearchCatalogState.Loading) }
    var filteredProducts by remember { mutableStateOf<List<ProductModel>>(emptyList()) }
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val cartViewModel: CartViewModel = viewModel(LocalContext.current as ComponentActivity)

    // Load the full product catalog once. This app's catalog is small enough that
    // filtering client-side is simpler and cheaper than standing up a real search
    // index (Algolia/Typesense) - if the catalog grows into the thousands, this is
    // the first thing to swap out.
    LaunchedEffect(Unit) {
        try {
            val snapshot = Firebase.firestore.collection("data").document("stock")
                .collection("products")
                .get()
                .await()
            val products = snapshot.documents.mapNotNull { it.toObject(ProductModel::class.java) }
            catalogState = SearchCatalogState.Loaded(products)
        } catch (e: Exception) {
            catalogState = SearchCatalogState.Error(e.localizedMessage ?: "Failed to load products")
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Debounce: wait for the user to stop typing before filtering, so fast typing
    // doesn't re-filter the whole list on every keystroke.
    LaunchedEffect(query, catalogState) {
        val loaded = catalogState as? SearchCatalogState.Loaded
        if (loaded == null || query.isBlank()) {
            filteredProducts = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        val q = query.trim().lowercase()
        filteredProducts = loaded.products.filter {
            it.title.lowercase().contains(q) || it.category.lowercase().contains(q)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("Search for vegetables, fruits...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                catalogState is SearchCatalogState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                catalogState is SearchCatalogState.Error -> {
                    val message = (catalogState as SearchCatalogState.Error).message
                    SearchEmptyState(
                        title = "Couldn't load products",
                        subtitle = message
                    )
                }

                query.isBlank() -> {
                    SearchEmptyState(
                        title = "Search for products",
                        subtitle = "Try \"tomato\", \"onion\", or a category name"
                    )
                }

                filteredProducts.isEmpty() -> {
                    SearchEmptyState(
                        title = "No results for \"$query\"",
                        subtitle = "Try a different name or category"
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                    ) {
                        items(
                            items = filteredProducts.chunked(2),
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
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun SearchEmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
