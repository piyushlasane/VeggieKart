package com.project.veggiekart.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.veggiekart.AppUtil
import com.project.veggiekart.model.AddressModel
import com.project.veggiekart.model.UserModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAddressesScreen(modifier: Modifier = Modifier, navController: NavHostController) {
    var addresses by remember { mutableStateOf<List<AddressModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf<AddressModel?>(null) }

    // Function to load addresses from Firestore
    fun loadAddresses() {
        scope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                try {
                    isLoading = true
                    val doc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .get()
                        .await()
                    val user = doc.toObject(UserModel::class.java)
                    addresses = user?.addresses ?: emptyList()
                } catch (e: Exception) {
                    AppUtil.showSnackbar(scope, snackbarHostState, "Error loading addresses")
                } finally {
                    isLoading = false
                    isUpdating = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadAddresses()
    }

    // Reload addresses when returning from add-address screen
    DisposableEffect(Unit) {
        val callback = navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Boolean>("address_updated")
            ?.observeForever { updated ->
                if (updated == true) {
                    loadAddresses()
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<Boolean>("address_updated")
                }
            }

        onDispose {
            // Cleanup if needed
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Addresses") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add-address") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Address")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (addresses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "No Addresses",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No addresses added yet",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to add your first address",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    items(addresses) { address ->
                        AddressCard(
                            address = address,
                            isUpdating = isUpdating,
                            onEdit = {
                                // TODO: Edit functionality
                            },
                            onDelete = {
                                showDeleteDialog = address
                            },
                            onSetDefault = {
                                if (!isUpdating) {
                                    isUpdating = true
                                    scope.launch {
                                        try {
                                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                                            if (uid != null) {
                                                // Update all addresses - set selected as default, others as non-default
                                                val updatedAddresses = addresses.map {
                                                    it.copy(isDefault = it.id == address.id)
                                                }

                                                // Update in Firestore
                                                FirebaseFirestore.getInstance()
                                                    .collection("users")
                                                    .document(uid)
                                                    .update("addresses", updatedAddresses)
                                                    .await()

                                                // Reload from Firestore to ensure consistency
                                                loadAddresses()

                                                AppUtil.showSnackbar(scope, snackbarHostState, "Default address updated")
                                            }
                                        } catch (e: Exception) {
                                            AppUtil.showSnackbar(scope, snackbarHostState, "Error updating address")
                                            isUpdating = false
                                        }
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Loading overlay when updating
                if (isUpdating) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        showDeleteDialog?.let { address ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete Address") },
                text = { Text("Are you sure you want to delete this address?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    isUpdating = true
                                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                                    if (uid != null) {
                                        val updatedAddresses = addresses.filter { it.id != address.id }

                                        // If deleted address was default and there are other addresses, set first as default
                                        val finalAddresses = if (address.isDefault && updatedAddresses.isNotEmpty()) {
                                            updatedAddresses.mapIndexed { index, addr ->
                                                if (index == 0) addr.copy(isDefault = true) else addr
                                            }
                                        } else {
                                            updatedAddresses
                                        }

                                        FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(uid)
                                            .update("addresses", finalAddresses)
                                            .await()

                                        // Reload addresses
                                        loadAddresses()

                                        AppUtil.showSnackbar(scope, snackbarHostState, "Address deleted")
                                    }
                                } catch (e: Exception) {
                                    AppUtil.showSnackbar(scope, snackbarHostState, "Error deleting address")
                                    isUpdating = false
                                } finally {
                                    showDeleteDialog = null
                                }
                            }
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AddressCard(
    address: AddressModel,
    isUpdating: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (address.isDefault)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = address.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = address.addressType,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (address.isDefault) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Default",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = address.phone,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = address.addressLine,
                fontSize = 14.sp
            )

            Text(
                text = "${address.city}, ${address.state} - ${address.pincode}",
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!address.isDefault) {
                    OutlinedButton(
                        onClick = onSetDefault,
                        modifier = Modifier.weight(1f),
                        enabled = !isUpdating
                    ) {
                        Text("Set as Default", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    enabled = !isUpdating
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !isUpdating
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 12.sp)
                }
            }
        }
    }
}