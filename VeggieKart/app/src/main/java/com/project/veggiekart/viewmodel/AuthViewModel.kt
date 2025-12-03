package com.project.veggiekart.viewmodel

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.project.veggiekart.model.UserModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AuthViewModel : ViewModel() {

    var mobileNumber by mutableStateOf("")
        private set

    var showOtp by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var otpValues = mutableStateListOf<String>().apply { repeat(6) { add("") } }
        private set

    fun updateMobileNumber(input: String) {
        if (input.length <= 10 && input.all { it.isDigit() }) {
            mobileNumber = input
        }
    }

    fun updateOtpAt(index: Int, value: String) {
        if (value.length <= 1 && value.all { it.isDigit() }) {
            otpValues[index] = value
        }
    }

    fun updateShowOtp(value: Boolean) {
        showOtp = value
    }

    fun updateLoading(value: Boolean) {
        isLoading = value
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendOtp(phoneNumber: String, activity: Activity, onResult: (Boolean, String) -> Unit) {
        val options = PhoneAuthOptions.newBuilder(auth).setPhoneNumber("+91$phoneNumber")
            .setTimeout(60L, TimeUnit.SECONDS).setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto verification
                    signInWithCredential(credential, onResult)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    onResult(false, e.localizedMessage ?: "Verification failed")
                }

                override fun onCodeSent(
                    verificationId: String, token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@AuthViewModel.verificationId = verificationId
                    this@AuthViewModel.resendToken = token
                    onResult(true, "OTP sent successfully")
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(
        code: String, onResult: (Boolean, String) -> Unit
    ) {
        val vid = verificationId
        if (vid == null) {
            onResult(false, "Verification ID not found. Please retry.")
            return
        }
        val credential = PhoneAuthProvider.getCredential(vid, code)
        signInWithCredential(credential, onResult)
    }

    private fun signInWithCredential(
        credential: PhoneAuthCredential, onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            auth.signInWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        checkOrCreateUser(user.uid, user.phoneNumber ?: "", onResult)
                    } else {
                        onResult(false, "Sign-in failed")
                    }
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Sign-in failed")
                }
            }
        }
    }

    private fun checkOrCreateUser(
        uid: String, phone: String, onResult: (Boolean, String) -> Unit
    ) {
        val userDoc = firestore.collection("users").document(uid)
        userDoc.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Existing user
                val user = snapshot.toObject(UserModel::class.java)
                if (user?.name.isNullOrEmpty()) {
                    onResult(true, "Please complete your profile")
                } else {
                    onResult(true, "Login successful")
                }
            } else {
                // New user - create without name
                val user = UserModel(uid = uid, phone = phone, name = "", createdAt = Timestamp.now())
                userDoc.set(user).addOnSuccessListener {
                    onResult(true, "Please complete your profile")
                }.addOnFailureListener {
                    onResult(false, "Failed to create user")
                }
            }
        }.addOnFailureListener {
            onResult(false, "Error checking user")
        }
    }
}
