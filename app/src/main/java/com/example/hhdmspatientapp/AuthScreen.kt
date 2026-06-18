package com.example.hhdmspatientapp

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.hhdmspatientapp.RetrofitClient
import com.example.hhdmspatientapp.SignupRequest
import com.example.hhdmspatientapp.LoginRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*

enum class ScreenState {
    LOGIN, SIGN_UP
}

fun splitFullName(fullName: String): Pair<String, String> {
    val cleanName = fullName.trim()
    val parts = cleanName.split("\\s+".toRegex())

    return when {
        cleanName.isEmpty() -> {
            Pair("Patient", "User")
        }
        parts.size == 1 -> {
            Pair(parts[0], ".")
        }
        else -> {
            val firstName = parts[0]
            val lastName = parts.drop(1).joinToString(" ")
            Pair(firstName, lastName)
        }
    }
}

@Composable
fun AuthScreen(onAuthSuccess: (String) -> Unit) {
    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.LOGIN) }

    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var inputErrorMsg by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WindowBackground)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = if (screenState == ScreenState.LOGIN) "Welcome Back" else "Create Account",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepCharcoal,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (screenState == ScreenState.LOGIN) "Sign in to access your dashboard" else "Join us to request medical appointments",
                fontSize = 14.sp,
                color = MutedTextGrey,
                modifier = Modifier.padding(top = 6.dp, bottom = 28.dp),
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    inputErrorMsg?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    if (screenState == ScreenState.SIGN_UP) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it; inputErrorMsg = null },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MedicalTeal) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) {
                                    phoneNumber = input
                                    inputErrorMsg = null
                                }
                            },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = MedicalTeal) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            placeholder = { Text("e.g., 01712345678") }
                        )
                    }

                    OutlinedTextField(
                        value = emailAddress,
                        onValueChange = { emailAddress = it; inputErrorMsg = null },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MedicalTeal) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it; inputErrorMsg = null },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MedicalTeal) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val visibilityIcon = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(visibilityIcon, contentDescription = null)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (emailAddress.isBlank() || passwordInput.isBlank()) {
                                inputErrorMsg = "Required inputs cannot be empty."
                            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
                                inputErrorMsg = "Please check your email formatting structure."
                            } else if (passwordInput.length < 6) {
                                inputErrorMsg = "Security rule: Password must exceed 5 letters."
                            } else if (screenState == ScreenState.SIGN_UP && fullName.isBlank()) {
                                inputErrorMsg = "Please supply your real identity name data."
                            } else if (screenState == ScreenState.SIGN_UP && phoneNumber.length < 11) {
                                inputErrorMsg = "Please enter a valid 11-digit phone number."
                            } else {
                                if (screenState == ScreenState.SIGN_UP) {
                                    val (firstName, lastName) = splitFullName(fullName)

                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val response = RetrofitClient.apiService.signup(
                                                SignupRequest(
                                                    first_name_en = firstName,
                                                    last_name_en = lastName,
                                                    phone_number = phoneNumber,
                                                    email = emailAddress,
                                                    password = passwordInput
                                                )
                                            )

                                            withContext(Dispatchers.Main) {
                                                android.util.Log.d("HHDMS_NET", "Registration Successful! Patient ID: ${response.patientId}")
                                                onAuthSuccess(emailAddress)
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                if (e is retrofit2.HttpException) {
                                                    val errorBodyString = e.response()?.errorBody()?.string()
                                                    android.util.Log.e("HHDMS_NET", "NestJS Validation Error Details: $errorBodyString")
                                                    inputErrorMsg = errorBodyString ?: "Validation check failed."
                                                } else {
                                                    android.util.Log.e("HHDMS_NET", "Network Connection Error", e)
                                                    inputErrorMsg = e.localizedMessage ?: "Connection to healthcare gateway failed."
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // =============================================================================
                                    // FIXED & UPDATED: CONNECTED LOGIN FLOW PIPELINE
                                    // =============================================================================
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val response = RetrofitClient.apiService.login(
                                                LoginRequest(
                                                    email = emailAddress.trim(),
                                                    password = passwordInput
                                                )
                                            )

                                            withContext(Dispatchers.Main) {
                                                inputErrorMsg = null // Clear old errors

                                                android.util.Log.d("HHDMS_NET", "Login Successful! Token: ${response.access_token}")
                                                android.util.Log.d("HHDMS_NET", "User Role System Scope: ${response.user.role}")

                                                onAuthSuccess(emailAddress) // Go to home dashboard
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                if (e is retrofit2.HttpException) {
                                                    val errorBodyString = e.response()?.errorBody()?.string()
                                                    android.util.Log.e("HHDMS_NET", "NestJS Login Validation Error: $errorBodyString")
                                                    inputErrorMsg = "Invalid identity email address or password combination."
                                                } else {
                                                    android.util.Log.e("HHDMS_NET", "Network Connection Error", e)
                                                    inputErrorMsg = e.localizedMessage ?: "Gateway gateway communication timeout error."
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalTeal)
                    ) {
                        Text(
                            text = if (screenState == ScreenState.LOGIN) "Sign In" else "Create Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (screenState == ScreenState.LOGIN) "New to our platform? " else "Already have an account? ",
                    color = MutedTextGrey,
                    fontSize = 14.sp
                )
                Text(
                    text = if (screenState == ScreenState.LOGIN) "Create an Account" else "Sign In Here",
                    color = MedicalTeal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            inputErrorMsg = null
                            screenState = if (screenState == ScreenState.LOGIN) ScreenState.SIGN_UP else ScreenState.LOGIN
                        }
                        .padding(4.dp)
                )
            }
        }
    }
}