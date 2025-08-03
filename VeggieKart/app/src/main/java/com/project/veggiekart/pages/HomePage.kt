package com.project.veggiekart.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.veggiekart.components.HeaderView

@Composable
fun HomePage(modifier: Modifier = Modifier) {
    Column (
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ){
        HeaderView()
    }
}