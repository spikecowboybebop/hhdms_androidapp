package com.example.hhdmspatientapp.nurse

import android.util.Log
import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.*
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NurseAuthScreen(onAuthSuccess: (email: String, role: String) -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var inputErrorMsg by remember { mutableStateOf<String?>(null) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    fun clearErrors() {
        inputErrorMsg = null; nameError = null; phoneError = null; emailError = null; passwordError = null
    }

    fun validate(): Boolean {
        clearErrors()
        var valid = true
        if (emailAddress.isBlank()) { emailError = "Email is required"; valid = false }
        else if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) { emailError = "Enter a valid email"; valid = false }
        if (passwordInput.isBlank()) { passwordError = "Password is required"; valid = false }
        else if (passwordInput.length < 6) { passwordError = "Password must be at least 6 characters"; valid = false }
        if (!isLogin) {
            if (fullName.isBlank()) { nameError = "Full name is required"; valid = false }
            if (phoneNumber.isBlank()) { phoneError = "Phone number is required"; valid = false }
            else if (phoneNumber.length < 10) { phoneError = "Enter a valid phone number"; valid = false }
        }
        return valid
    }

    fun submit() {
        if (!validate()) return
        isLoading = true
        inputErrorMsg = null

        if (!isLogin) {
            val parts = fullName.trim().split("\\s+".toRegex())
            val firstName = parts.getOrElse(0) { "" }
            val lastName = if (parts.size > 1) parts.drop(1).joinToString(" ") else "."
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.apiService.signup(
                        SignupRequest(
                            first_name_en = firstName,
                            last_name_en = lastName,
                            phone_number = if (phoneNumber.length == 10) "0$phoneNumber" else phoneNumber,
                            email = emailAddress,
                            password = passwordInput,
                            role_name = "NURSE",
                        ),
                    )
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        Log.d("NurseAuth", "Registration successful: ${response.patientId}")
                        val token = response.access_token
                        if (token != null) {
                            TokenManager.saveToken(token)
                        }
                        NurseFirebaseMessagingService.registerCurrentToken()
                        onAuthSuccess(emailAddress, "NURSE")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        inputErrorMsg = if (e is retrofit2.HttpException) {
                            parseServerError(e.response()?.errorBody()?.string())
                        } else {
                            "Unable to connect. Please check your network."
                        }
                    }
                }
            }
        } else {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.apiService.login(
                        LoginRequest(email = emailAddress.trim(), password = passwordInput),
                    )
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        Log.d("NurseAuth", "Login successful: ${response.user.role}")
                        TokenManager.saveToken(response.access_token)
                        NurseFirebaseMessagingService.registerCurrentToken()
                        onAuthSuccess(emailAddress, response.user.role)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        inputErrorMsg = if (e is retrofit2.HttpException) {
                            parseServerError(e.response()?.errorBody()?.string())
                        } else {
                            "Unable to connect. Please check your network."
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(TechTeal, ClinicalNavy))
    )) {
        Text(
            text = "Aastha Nurse\nCare Portal",
            color = PureWhite,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 42.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 100.dp, start = 28.dp, end = 28.dp),
        )

        Column(
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                .align(Alignment.BottomCenter)
                .background(PureWhite, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .verticalScroll(rememberScrollState()).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.padding(top = 12.dp).width(40.dp).height(4.dp)
                .background(LightGray, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(visible = inputErrorMsg != null, enter = fadeIn(), exit = fadeOut()) {
                inputErrorMsg?.let {
                    Text(it, color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                if (isLogin) {
                    Text("Nurse Login", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)) {
                        Text("No account? ", color = CoolGray, fontSize = 14.sp)
                        Text("Sign Up", color = TechTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { clearErrors(); isLogin = false }.padding(4.dp))
                    }

                    // Login fields
                    OutlinedTextField(value = emailAddress, onValueChange = { emailAddress = it; emailError = null },
                        label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null, tint = IconMuted) },
                        isError = emailError != null, supportingText = emailError?.let { { Text(it, color = ErrorRed) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp), shape = RoundedCornerShape(14.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg,
                            focusedTextColor = TitleBlack, unfocusedTextColor = TitleBlack, focusedBorderColor = TechTeal,
                            unfocusedBorderColor = LightGray, cursorColor = TechTeal, focusedLabelColor = TechTeal, unfocusedLabelColor = IconMuted))

                    OutlinedTextField(value = passwordInput, onValueChange = { passwordInput = it; passwordError = null },
                        label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, null, tint = IconMuted) },
                        trailingIcon = { IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = CoolGray) } },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = passwordError != null, supportingText = passwordError?.let { { Text(it, color = ErrorRed) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), shape = RoundedCornerShape(14.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg,
                            focusedTextColor = TitleBlack, unfocusedTextColor = TitleBlack, focusedBorderColor = TechTeal,
                            unfocusedBorderColor = LightGray, cursorColor = TechTeal, focusedLabelColor = TechTeal, unfocusedLabelColor = IconMuted))

                } else {
                    Text("Nurse Sign Up", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TitleBlack)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)) {
                        Text("Have an account? ", color = CoolGray, fontSize = 14.sp)
                        Text("Login", color = TechTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { clearErrors(); isLogin = true }.padding(4.dp))
                    }

                    // Signup fields
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it; nameError = null },
                        label = { Text("Full Name") }, leadingIcon = { Icon(Icons.Default.Person, null, tint = IconMuted) },
                        isError = nameError != null, supportingText = nameError?.let { { Text(it, color = ErrorRed) } },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(14.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg,
                            focusedTextColor = TitleBlack, unfocusedTextColor = TitleBlack, focusedBorderColor = TechTeal,
                            unfocusedBorderColor = LightGray, cursorColor = TechTeal, focusedLabelColor = TechTeal, unfocusedLabelColor = IconMuted))

                    OutlinedTextField(value = phoneNumber, onValueChange = { if (it.all { c -> c.isDigit() }) { phoneNumber = it; phoneError = null } },
                        label = { Text("Phone Number") }, leadingIcon = { Icon(Icons.Default.Phone, null, tint = IconMuted) },
                        isError = phoneError != null, supportingText = phoneError?.let { { Text(it, color = ErrorRed) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(14.dp), singleLine = true,
                        prefix = { Text("+880 ", color = CoolGray, fontWeight = FontWeight.Medium) },
                        placeholder = { Text("1712345678") },
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg,
                            focusedTextColor = TitleBlack, unfocusedTextColor = TitleBlack, focusedBorderColor = TechTeal,
                            unfocusedBorderColor = LightGray, cursorColor = TechTeal, focusedLabelColor = TechTeal, unfocusedLabelColor = IconMuted))

                    OutlinedTextField(value = emailAddress, onValueChange = { emailAddress = it; emailError = null },
                        label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null, tint = IconMuted) },
                        isError = emailError != null, supportingText = emailError?.let { { Text(it, color = ErrorRed) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(14.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg,
                            focusedTextColor = TitleBlack, unfocusedTextColor = TitleBlack, focusedBorderColor = TechTeal,
                            unfocusedBorderColor = LightGray, cursorColor = TechTeal, focusedLabelColor = TechTeal, unfocusedLabelColor = IconMuted))

                    OutlinedTextField(value = passwordInput, onValueChange = { passwordInput = it; passwordError = null },
                        label = { Text("Create Password") }, leadingIcon = { Icon(Icons.Default.Lock, null, tint = IconMuted) },
                        trailingIcon = { IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = CoolGray) } },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = passwordError != null, supportingText = passwordError?.let { { Text(it, color = ErrorRed) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(14.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg,
                            focusedTextColor = TitleBlack, unfocusedTextColor = TitleBlack, focusedBorderColor = TechTeal,
                            unfocusedBorderColor = LightGray, cursorColor = TechTeal, focusedLabelColor = TechTeal, unfocusedLabelColor = IconMuted))
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(onClick = { submit() }, enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TechTeal, disabledContainerColor = TechTeal.copy(alpha = 0.5f),
                        contentColor = PureWhite, disabledContentColor = PureWhite.copy(alpha = 0.7f))) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = PureWhite, strokeWidth = 2.dp)
                    else Text(if (isLogin) "LOGIN" else "REGISTER", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

private fun parseServerError(errorBody: String?): String {
    if (errorBody == null) return "Something went wrong."
    return try {
        val json = org.json.JSONObject(errorBody)
        json.optString("message", json.optString("error", "Something went wrong."))
    } catch (_: Exception) { errorBody }
}
