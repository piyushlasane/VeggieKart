package com.project.veggiekart.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.veggiekart.AddressUpdateNotifier
import com.project.veggiekart.AppUtil
import com.project.veggiekart.model.AddressModel
import com.project.veggiekart.model.UserModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAddressScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    addressId: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // User details
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Address details
    var houseNo by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var addressType by remember { mutableStateOf("Home") }
    var isDefault by remember { mutableStateOf(false) }

    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }

    var isSaving by remember { mutableStateOf(false) }
    var isDetectingLocation by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Check location permission
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // Pre-fill user data
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            try {
                val doc =
                    FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                val user = doc.toObject(UserModel::class.java)
                name = user?.name ?: ""
                phone = user?.phone?.removePrefix("+91") ?: ""
                isDefault = user?.addresses.isNullOrEmpty()

                // Load address data if editing
                if (addressId != null) {
                    val address = user?.addresses?.find { it.id == addressId }
                    if (address != null) {
                        name = address.name
                        phone = address.phone.removePrefix("+91")
                        val parts = address.addressLine.split(",")
                        houseNo = parts.getOrNull(0)?.trim() ?: ""
                        area = parts.getOrNull(1)?.trim() ?: ""
                        landmark = address.landmark
                        city = address.city
                        state = address.state
                        pincode = address.pincode
                        addressType = address.addressType
                        isDefault = address.isDefault
                        latitude = address.latitude
                        longitude = address.longitude
                    }
                }

            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            // Detect location
            detectCurrentLocation(context) { lat, lng, address ->
                latitude = lat
                longitude = lng
                city = address.locality ?: ""
                state = address.adminArea ?: ""
                pincode = address.postalCode ?: ""
                area = address.subLocality ?: address.featureName ?: ""

                isDetectingLocation = false
                AppUtil.showSnackbar(scope, snackbarHostState, "Location detected successfully")
            }
        } else {
            isDetectingLocation = false
            AppUtil.showSnackbar(scope, snackbarHostState, "Location permission denied")
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(if (addressId != null) "Edit Address" else "Add Delivery Address") }, navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        })
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            // Detect Location Button
            Card(
                modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Use Current Location",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Auto-detect your address",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            if (hasLocationPermission) {
                                isDetectingLocation = true
                                detectCurrentLocation(context) { lat, lng, address ->
                                    latitude = lat
                                    longitude = lng
                                    city = address.locality ?: ""
                                    state = address.adminArea ?: ""
                                    pincode = address.postalCode ?: ""
                                    area = address.subLocality ?: address.featureName ?: ""

                                    isDetectingLocation = false
                                    AppUtil.showSnackbar(
                                        scope, snackbarHostState, "Location detected!"
                                    )
                                }
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }, enabled = !isDetectingLocation
                    ) {
                        if (isDetectingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp), strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = "Detect",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Detect")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Address Type Selection
            Text(
                text = "Save Address As", fontSize = 14.sp, fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Home", "Work", "Other").forEach { type ->
                    FilterChip(
                        selected = addressType == type,
                        onClick = { addressType = type },
                        label = { Text(type) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contact Details
            Text(
                text = "Contact Details", fontSize = 16.sp, fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Address Details
            Text(
                text = "Address Details", fontSize = 16.sp, fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = houseNo,
                onValueChange = { houseNo = it },
                label = { Text("House / Flat / Block No. *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = area,
                onValueChange = { area = it },
                label = { Text("Area / Sector / Locality *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = landmark,
                onValueChange = { landmark = it },
                label = { Text("Nearby Landmark (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pincode,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) pincode = it
                    },
                    label = { Text("Pincode *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isDefault, onCheckedChange = { isDefault = it })
                Text("Make this my default address", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // Validation
                    when {
                        name.trim().isEmpty() -> {
                            AppUtil.showSnackbar(scope, snackbarHostState, "Please enter name")
                            return@Button
                        }

                        phone.trim().isEmpty() || phone.trim().length != 10 -> {
                            AppUtil.showSnackbar(
                                scope, snackbarHostState, "Please enter valid phone number"
                            )
                            return@Button
                        }

                        houseNo.trim().isEmpty() -> {
                            AppUtil.showSnackbar(
                                scope, snackbarHostState, "Please enter house/flat number"
                            )
                            return@Button
                        }

                        area.trim().isEmpty() -> {
                            AppUtil.showSnackbar(
                                scope, snackbarHostState, "Please enter area/locality"
                            )
                            return@Button
                        }

                        city.trim().isEmpty() -> {
                            AppUtil.showSnackbar(scope, snackbarHostState, "Please enter city")
                            return@Button
                        }

                        state.trim().isEmpty() -> {
                            AppUtil.showSnackbar(scope, snackbarHostState, "Please enter state")
                            return@Button
                        }

                        pincode.trim().length != 6 -> {
                            AppUtil.showSnackbar(
                                scope, snackbarHostState, "Please enter valid pincode"
                            )
                            return@Button
                        }
                    }
                    isSaving = true
                    scope.launch {
                        try {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            if (uid != null) {
                                val userDoc = FirebaseFirestore.getInstance().collection("users")
                                    .document(uid)

                                val doc = userDoc.get().await()
                                val user = doc.toObject(UserModel::class.java)
                                val currentAddresses = user?.addresses ?: emptyList()

                                val fullAddress = buildString {
                                    append(houseNo.trim())
                                    if (area.trim().isNotEmpty()) append(", ${area.trim()}")
                                    if (landmark.trim()
                                            .isNotEmpty()
                                    ) append(", Near ${landmark.trim()}")
                                }

                                val newAddress = AddressModel(
                                    id = addressId ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    addressLine = fullAddress,
                                    landmark = landmark.trim(),
                                    city = city.trim(),
                                    state = state.trim(),
                                    pincode = pincode.trim(),
                                    latitude = latitude,
                                    longitude = longitude,
                                    addressType = addressType,
                                    isDefault = isDefault || currentAddresses.isEmpty()
                                )

                                // Remove old address if editing
                                val addressesWithoutCurrent = if (addressId != null) {
                                    currentAddresses.filter { it.id != addressId }
                                } else {
                                    currentAddresses
                                }

                                // If new address is default, make others non-default
                                val updatedAddresses = if (newAddress.isDefault) {
                                    addressesWithoutCurrent.map { it.copy(isDefault = false) } + newAddress
                                } else {
                                    addressesWithoutCurrent + newAddress
                                }

                                userDoc.update("addresses", updatedAddresses).await()
                                AddressUpdateNotifier.notifyAddressUpdated()
                                AppUtil.showSnackbar(
                                    scope, snackbarHostState,
                                    if (addressId != null) "Address updated" else "Address saved"
                                )
                                kotlinx.coroutines.delay(1000)
                                navController.navigateUp()
                            }
                        } catch (e: Exception) {
                            AppUtil.showSnackbar(
                                scope, snackbarHostState, "Error: ${e.localizedMessage}"
                            )
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Address", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Helper function to detect current location
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun detectCurrentLocation(
    context: android.content.Context, onLocation: (Double, Double, android.location.Address) -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
// Reverse geocode
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    geocoder.getFromLocation(
                        location.latitude, location.longitude, 1
                    ) { addresses ->
                        if (addresses.isNotEmpty()) {
                            onLocation(location.latitude, location.longitude, addresses[0])
                        }
                    }
                } catch (e: Exception) {
// Handle geocoding error
                }
            }
        }
    } catch (e: SecurityException) {
// Handle permission error
    }
}
