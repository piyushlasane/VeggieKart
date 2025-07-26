package com.project.veggiekart.screens

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.navigation.NavHostController
import com.project.veggiekart.R

@Composable
fun LoginScreen(modifier: Modifier = Modifier, navController: NavHostController) {

    var mobileNumber by remember { mutableStateOf("") }
    var showOtp by remember { mutableStateOf(false) }

    val correctOtp = "1234"
    val otpLength = 4
    val otpValues = remember { mutableStateListOf(*Array(otpLength) { "" }) }
    val focusRequesters = List(otpLength) { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Login / Signup",
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
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
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )

        Spacer(Modifier.height(16.dp))

        if (!showOtp) {
            Button(
                onClick = { showOtp = true },
                enabled = mobileNumber.length == 10
            ) { Text("Send OTP") }
        } else {
            // OTP UI
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                otpValues.forEachIndexed { index, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { input ->
                            if (input.length <= 1 && input.all { it.isDigit() }) {
                                otpValues[index] = input
                                if (input.isNotEmpty() && index < otpLength - 1) {
                                    focusRequesters[index + 1].requestFocus()
                                }
                                if (index == otpLength - 1) {
                                    keyboardController?.hide()
                                }
                            }
                        },
                        modifier = Modifier
                            .width(54.dp)
                            .focusRequester(focusRequesters[index]),
                        textStyle = TextStyle(
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val enteredOtp = otpValues.joinToString("")
                    if (enteredOtp == correctOtp) {
                        navController.navigate("homescreen")
                    }
                },
                enabled = otpValues.all { it.isNotEmpty() },
                modifier = Modifier.width(250.dp).height(45.dp)
            ) { Text("Verify & Proceed", fontSize = 16.sp) }

            LaunchedEffect(Unit) {
                focusRequesters[0].requestFocus()
            }
        }
    }
}
