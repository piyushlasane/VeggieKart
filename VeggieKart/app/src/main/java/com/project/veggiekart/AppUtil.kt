package com.project.veggiekart

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.State

object AppUtil {
    fun showSnackbar(
        scope: CoroutineScope, snackbarHostState: SnackbarHostState, message: String
    ) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
}

object AddressUpdateNotifier {
    private var _updateTrigger = mutableStateOf(0)
    val updateTrigger: State<Int> = _updateTrigger

    fun notifyAddressUpdated() {
        _updateTrigger.value++
    }
}