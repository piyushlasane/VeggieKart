package com.project.veggiekart.pages

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.veggiekart.components.BannerView
import com.project.veggiekart.components.CategoriesView
import com.project.veggiekart.components.HeaderView
import com.project.veggiekart.viewmodel.CartViewModel

@Composable
fun HomePage(modifier: Modifier = Modifier, snackbarHostState: SnackbarHostState) {
    val scrollState = rememberScrollState()
    // Activity-scoped so this resolves to the SAME CartViewModel instance as every
    // other screen - loaded once (see HomeScreen), no reload needed here.
    val cartViewModel: CartViewModel = viewModel(LocalContext.current as ComponentActivity)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
    ) {
        HeaderView(modifier)
        Spacer(modifier = Modifier.height(10.dp))
        BannerView(modifier = Modifier.height(150.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Categories",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        )
        Spacer(modifier = Modifier.height(20.dp))
        CategoriesView(modifier)
        Spacer(modifier = Modifier.height(20.dp))
    }
}