package com.project.veggiekart.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.project.veggiekart.AppUtil
import com.project.veggiekart.R
import com.project.veggiekart.checkProfileComplete
import com.project.veggiekart.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(modifier: Modifier = Modifier, navController: NavHostController, authViewModel: AuthViewModel = viewModel()) {

    val mobileNumber = authViewModel.mobileNumber
    val showOtp = authViewModel.showOtp
    val isLoading = authViewModel.isLoading
    val otpLength = 6
    val otpValues = authViewModel.otpValues
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequesters = List(otpLength) { FocusRequester() }
    val context = LocalContext.current

    // ViewModel and snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Text(
                text = "Login / Signup", modifier = Modifier.fillMaxWidth(), style = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.dmsans_bold))
                )
            )

            Spacer(Modifier.height(16.dp))

            Image(
                painter = painterResource(R.drawable.login_banner),
                contentDescription = "Login Banner",
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = mobileNumber,
                onValueChange = { authViewModel.updateMobileNumber(it) },
                label = { Text("Mobile Number (+91)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                )
            )

            Spacer(Modifier.height(16.dp))

            if (!showOtp) {
                Button(
                    onClick = {
                        authViewModel.updateLoading(true)
                        authViewModel.sendOtp(
                            phoneNumber = mobileNumber,
                            activity = context as Activity
                        ) { success, message ->
                            authViewModel.updateLoading(false)
                            AppUtil.showSnackbar(scope, snackbarHostState, message)
                            if (success) authViewModel.updateShowOtp(true)
                        }
                    },
                    enabled = mobileNumber.length == 10 && !isLoading,
                    modifier = Modifier
                        .width(150.dp)
                        .height(45.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp), strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send OTP", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                // OTP UI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    otpValues.forEachIndexed { index, value ->
                        OutlinedTextField(
                            value = value,
                            onValueChange = { input ->
                                if (input.length <= 1 && input.all { it.isDigit() }) {
                                    authViewModel.updateOtpAt(index, input)
                                    if (input.isNotEmpty() && index < otpLength - 1) {
                                        focusRequesters[index + 1].requestFocus()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequesters[index])
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown && keyEvent.key == androidx.compose.ui.input.key.Key.Backspace) {
                                        // If current box is empty, go back
                                        if (otpValues[index].isEmpty() && index > 0) {
                                            focusRequesters[index - 1].requestFocus()
                                            otpValues[index - 1] = "" // also clear previous value
                                            true
                                        } else {
                                            false
                                        }
                                    } else {
                                        false
                                    }
                                },
                            textStyle = TextStyle(
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        val enteredOtp = otpValues.joinToString("")
                        authViewModel.updateLoading(true)
                        authViewModel.verifyOtp(enteredOtp) { success, message ->
                            authViewModel.updateLoading(false)
                            AppUtil.showSnackbar(scope, snackbarHostState, message)
                            if (success) {
                                // Check if profile is complete before navigating
                                scope.launch {
                                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                    if (uid != null) {
                                        val isComplete = checkProfileComplete(uid)
                                        val destination = if (isComplete) "home" else "complete-profile"
                                        navController.navigate(destination) {
                                            popUpTo("auth") { inclusive = true }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    enabled = otpValues.all { it.isNotEmpty() } && !isLoading,
                    modifier = Modifier
                        .width(250.dp)
                        .height(45.dp)) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp), strokeWidth = 2.dp
                        )
                    } else {
                        Text("Verify & Proceed", fontSize = 16.sp)
                    }
                }

                LaunchedEffect(Unit) {
                    focusRequesters[0].requestFocus()
                }
            }
        }
        // Snackbar for messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
        )
    }
}
