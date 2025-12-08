package com.project.veggiekart

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object AppUtil {
    fun showSnackbar(
        scope: CoroutineScope, snackbarHostState: SnackbarHostState, message: String
    ) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
}

object AddressUpdateNotifier {
    private var updateTrigger = mutableStateOf(0)

    fun notifyAddressUpdated() {
        updateTrigger.value++
    }

    fun getUpdateTrigger() = updateTrigger.value
}