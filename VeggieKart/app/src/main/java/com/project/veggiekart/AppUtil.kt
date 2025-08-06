package com.project.veggiekart

import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object AppUtil {
    fun showSnackbar(
        scope: CoroutineScope, snackbarHostState: SnackbarHostState, message: String
    ) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
}
