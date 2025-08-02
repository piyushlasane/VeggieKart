package com.project.veggiekart.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import com.project.veggiekart.R
import com.project.veggiekart.ui.theme.Purple
import com.project.veggiekart.ui.theme.Purple40
import com.project.veggiekart.ui.theme.Purple80
import com.project.veggiekart.ui.theme.VeggieGreen
import com.project.veggiekart.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(modifier: Modifier = Modifier, navController: NavHostController) {

    var mobileNumber by remember { mutableStateOf("") }
    var showOtp by remember { mutableStateOf(false) }

    val otpLength = 6
    val otpValues = remember { mutableStateListOf(*Array(otpLength) { "" }) }
    val focusRequesters = List(otpLength) { FocusRequester() }
    LocalSoftwareKeyboardController.current

    // ViewModel and snackbar
    val authViewModel: AuthViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var verificationInProgress by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Snackbar for messages
        SnackbarHost(hostState = snackbarHostState)

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
            onValueChange = { input ->
                if (input.length <= 10 && input.all { it.isDigit() }) {
                    mobileNumber = input
                }
            },
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
                    verificationInProgress = true
                    authViewModel.sendOtp(
                        phoneNumber = mobileNumber,
                        activity = (navController.context as ComponentActivity)
                    ) { success, message ->
                        verificationInProgress = false
                        scope.launch { snackbarHostState.showSnackbar(message) }
                        if (success) showOtp = true
                    }
                }, enabled = mobileNumber.length == 10 && !verificationInProgress,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple,
                    contentColor = Color.White
                )
            ) { Text("Send OTP", fontWeight = FontWeight.SemiBold) }
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
                                otpValues[index] = input
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
                val enteredOtp = otpValues.joinToString("")
                verificationInProgress = true
                authViewModel.verifyOtp(enteredOtp) { success, message ->
                    verificationInProgress = false
                    scope.launch { snackbarHostState.showSnackbar(message) }
                    if (success) {
                        navController.navigate("homescreen") {
                            popUpTo("loginscreen") { inclusive = true }
                        }
                    }
                }
            },
                enabled = otpValues.all { it.isNotEmpty() } && !verificationInProgress,
                modifier = Modifier
                    .width(250.dp)
                    .height(45.dp)) {
                Text(
                    "Verify & Proceed", fontSize = 16.sp
                )
            }

            LaunchedEffect(Unit) {
                focusRequesters[0].requestFocus()
            }
        }
    }
}
