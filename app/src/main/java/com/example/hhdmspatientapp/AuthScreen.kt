package com.example.hhdmspatientapp

import android.util.Log
import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hhdmspatientapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ScreenState {
    LOGIN, SIGN_UP
}

fun splitFullName(fullName: String): Pair<String, String> {
    val cleanName = fullName.trim()
    val parts = cleanName.split("\\s+".toRegex())
    return when {
        cleanName.isEmpty() -> Pair("Patient", "User")
        parts.size == 1 -> Pair(parts[0], ".")
        else -> Pair(parts[0], parts.drop(1).joinToString(" "))
    }
}

// ─────────────────────────────────────────────────────────────────
// Geometric A logo
// ─────────────────────────────────────────────────────────────────
@Composable
fun GeometricLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.1f

        drawLine(
            color = ClinicalNavy,
            start = Offset(w * 0.5f, h * 0.08f),
            end = Offset(w * 0.08f, h * 0.92f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = ClinicalNavy,
            start = Offset(w * 0.5f, h * 0.08f),
            end = Offset(w * 0.92f, h * 0.92f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = TechTeal,
            start = Offset(w * 0.2f, h * 0.6f),
            end = Offset(w * 0.8f, h * 0.6f),
            strokeWidth = stroke * 0.75f,
            cap = StrokeCap.Round,
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Tab toggle
// ─────────────────────────────────────────────────────────────────
@Composable
fun AuthTabToggle(
    isLogin: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicatorPosition by animateFloatAsState(
        targetValue = if (isLogin) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "tabIndicator",
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(horizontal = 40.dp)) {
        val tabWidth = maxWidth / 2

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .clickable { onToggle(true) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "LOGIN",
                    color = if (isLogin) TechTeal else CoolGray,
                    fontWeight = if (isLogin) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp,
                )
            }
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .clickable { onToggle(false) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "SIGN UP",
                    color = if (!isLogin) TechTeal else CoolGray,
                    fontWeight = if (!isLogin) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp,
                )
            }
        }

        Box(
            modifier = Modifier
                .offset(x = tabWidth * indicatorPosition)
                .width(tabWidth)
                .padding(horizontal = 28.dp)
                .height(3.dp)
                .background(TechTeal, RoundedCornerShape(2.dp))
                .align(Alignment.BottomCenter),
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// ─────────────────────────────────────────────────────────────────
// Login form content
// ─────────────────────────────────────────────────────────────────
@Composable
fun LoginContent(
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: String?,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordError: String?,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email Address") },
        leadingIcon = { Icon(Icons.Default.Email, null, tint = IconMuted) },
        isError = emailError != null,
        supportingText = emailError?.let { { Text(it, color = ErrorRed) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedTextColor = TitleBlack,
            unfocusedTextColor = TitleBlack,
            focusedBorderColor = TechTeal,
            unfocusedBorderColor = LightGray,
            cursorColor = TechTeal,
            focusedLabelColor = TechTeal,
            unfocusedLabelColor = IconMuted,
        ),
    )

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        leadingIcon = { Icon(Icons.Default.Lock, null, tint = IconMuted) },
        trailingIcon = {
            val icon = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
            IconButton(onClick = onTogglePasswordVisibility) {
                Icon(icon, null, tint = CoolGray)
            }
        },
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        isError = passwordError != null,
        supportingText = passwordError?.let { { Text(it, color = ErrorRed) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedTextColor = TitleBlack,
            unfocusedTextColor = TitleBlack,
            focusedBorderColor = TechTeal,
            unfocusedBorderColor = LightGray,
            cursorColor = TechTeal,
            focusedLabelColor = TechTeal,
            unfocusedLabelColor = IconMuted,
        ),
    )

    Box(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 20.dp)) {
        Text(
            text = "Forgot Password?",
            color = TechTeal,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterEnd).clickable { },
        )
    }

    Button(
        onClick = onSubmit,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TechTeal,
            disabledContainerColor = TechTeal.copy(alpha = 0.5f),
            contentColor = PureWhite,
            disabledContentColor = PureWhite.copy(alpha = 0.7f),
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = PureWhite,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = "LOGIN",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = PureWhite,
            )
        }
    }

}

// ─────────────────────────────────────────────────────────────────
// Sign up form content
// ─────────────────────────────────────────────────────────────────
@Composable
fun SignUpContent(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    nameError: String?,
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    phoneError: String?,
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: String?,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordError: String?,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
) {
    OutlinedTextField(
        value = fullName,
        onValueChange = onFullNameChange,
        label = { Text("Full Name") },
        leadingIcon = { Icon(Icons.Default.Person, null, tint = IconMuted) },
        isError = nameError != null,
        supportingText = nameError?.let { { Text(it, color = ErrorRed) } },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedTextColor = TitleBlack,
            unfocusedTextColor = TitleBlack,
            focusedBorderColor = TechTeal,
            unfocusedBorderColor = LightGray,
            cursorColor = TechTeal,
            focusedLabelColor = TechTeal,
            unfocusedLabelColor = IconMuted,
        ),
    )

    OutlinedTextField(
        value = phoneNumber,
        onValueChange = onPhoneChange,
        label = { Text("Mobile Phone Number") },
        leadingIcon = { Icon(Icons.Default.Phone, null, tint = IconMuted) },
        isError = phoneError != null,
        supportingText = phoneError?.let { { Text(it, color = ErrorRed) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        prefix = { Text("+880 ", color = CoolGray, fontWeight = FontWeight.Medium) },
        placeholder = { Text("1712345678") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedTextColor = TitleBlack,
            unfocusedTextColor = TitleBlack,
            focusedBorderColor = TechTeal,
            unfocusedBorderColor = LightGray,
            cursorColor = TechTeal,
            focusedLabelColor = TechTeal,
            unfocusedLabelColor = IconMuted,
        ),
    )

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email Address") },
        leadingIcon = { Icon(Icons.Default.Email, null, tint = IconMuted) },
        isError = emailError != null,
        supportingText = emailError?.let { { Text(it, color = ErrorRed) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedTextColor = TitleBlack,
            unfocusedTextColor = TitleBlack,
            focusedBorderColor = TechTeal,
            unfocusedBorderColor = LightGray,
            cursorColor = TechTeal,
            focusedLabelColor = TechTeal,
            unfocusedLabelColor = IconMuted,
        ),
    )

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Create Password") },
        leadingIcon = { Icon(Icons.Default.Lock, null, tint = IconMuted) },
        trailingIcon = {
            val icon = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
            IconButton(onClick = onTogglePasswordVisibility) {
                Icon(icon, null, tint = CoolGray)
            }
        },
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        isError = passwordError != null,
        supportingText = passwordError?.let { { Text(it, color = ErrorRed) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            focusedTextColor = TitleBlack,
            unfocusedTextColor = TitleBlack,
            focusedBorderColor = TechTeal,
            unfocusedBorderColor = LightGray,
            cursorColor = TechTeal,
            focusedLabelColor = TechTeal,
            unfocusedLabelColor = IconMuted,
        ),
    )

    Spacer(modifier = Modifier.height(4.dp))

    Button(
        onClick = onSubmit,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TechTeal,
            disabledContainerColor = TechTeal.copy(alpha = 0.5f),
            contentColor = PureWhite,
            disabledContentColor = PureWhite.copy(alpha = 0.7f),
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = PureWhite,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = "REGISTER",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = PureWhite,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Main auth screen — bottom pill panel with slide animation
// ─────────────────────────────────────────────────────────────────
@Composable
fun AuthScreen(onAuthSuccess: (String) -> Unit) {
    var screenState by remember { mutableStateOf(ScreenState.LOGIN) }
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

    val isLogin = screenState == ScreenState.LOGIN

    fun clearErrors() {
        inputErrorMsg = null
        nameError = null
        phoneError = null
        emailError = null
        passwordError = null
    }

    fun validate(): Boolean {
        clearErrors()
        var valid = true

        if (emailAddress.isBlank()) {
            emailError = "Email is required"
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            emailError = "Enter a valid email address"
            valid = false
        }

        if (passwordInput.isBlank()) {
            passwordError = "Password is required"
            valid = false
        } else if (passwordInput.length < 6) {
            passwordError = "Password must be at least 6 characters"
            valid = false
        }

        if (!isLogin) {
            if (fullName.isBlank()) {
                nameError = "Full name is required"
                valid = false
            }
            if (phoneNumber.isBlank()) {
                phoneError = "Phone number is required"
                valid = false
            } else if (phoneNumber.length < 11) {
                phoneError = "Enter a valid 11-digit phone number"
                valid = false
            }
        }

        return valid
    }

    fun submit() {
        if (!validate()) return
        isLoading = true
        inputErrorMsg = null

        if (!isLogin) {
            val (firstName, lastName) = splitFullName(fullName)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.apiService.signup(
                        SignupRequest(
                            first_name_en = firstName,
                            last_name_en = lastName,
                            phone_number = phoneNumber,
                            email = emailAddress,
                            password = passwordInput,
                        ),
                    )
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        Log.d("HHDMS_NET", "Registration successful: ${response.patientId}")
                        onAuthSuccess(emailAddress)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        inputErrorMsg = if (e is retrofit2.HttpException) {
                            parseServerError(e.response()?.errorBody()?.string())
                        } else {
                            "Unable to connect. Please check your network and try again."
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
                        Log.d("HHDMS_NET", "Login successful: ${response.user.role}")
                        onAuthSuccess(emailAddress)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        inputErrorMsg = if (e is retrofit2.HttpException) {
                            parseServerError(e.response()?.errorBody()?.string())
                        } else {
                            "Unable to connect. Please check your network and try again."
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(
            colors = listOf(
                TechTeal,
                ClinicalNavy,
            ),
        )
    )) {
        // ── Welcome title ──
        Text(
            text = "Welcome to Aastha\nTele-HealthCare",
            color = PureWhite,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 42.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 120.dp, start = 28.dp, end = 28.dp),
        )

        // ── Pill panel ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.BottomCenter)
                .background(PureWhite, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(LightGray, RoundedCornerShape(2.dp)),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Global error ──
            AnimatedVisibility(
                visible = inputErrorMsg != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                inputErrorMsg?.let {
                    Text(
                        text = it,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
            }

            // ── Animated pill-switching form content ──
            // Old pill slides down, new pill slides up from bottom
            AnimatedContent(
                targetState = isLogin,
                transitionSpec = {
                    slideInVertically { height -> height } + fadeIn() togetherWith
                        slideOutVertically { height -> height } + fadeOut()
                },
                label = "pillSwitch",
            ) { login ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (login) {
                        // ── Login header ──
                        Text(
                            text = "Login",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TitleBlack,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                        ) {
                            Text(
                                text = "Don't Have An Account? ",
                                color = CoolGray,
                                fontSize = 14.sp,
                            )
                            Text(
                                text = "Sign Up",
                                color = TechTeal,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        clearErrors()
                                        screenState = ScreenState.SIGN_UP
                                    }
                                    .padding(4.dp),
                            )
                        }

                        LoginContent(
                            email = emailAddress,
                            onEmailChange = { emailAddress = it; emailError = null },
                            emailError = emailError,
                            password = passwordInput,
                            onPasswordChange = { passwordInput = it; passwordError = null },
                            passwordError = passwordError,
                            isPasswordVisible = isPasswordVisible,
                            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                            isLoading = isLoading,
                            onSubmit = { submit() },
                            focusManager = focusManager,
                        )
                    } else {
                        // ── Sign Up header ──
                        Text(
                            text = "Sign Up",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TitleBlack,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                        ) {
                            Text(
                                text = "Already Have An Account? ",
                                color = CoolGray,
                                fontSize = 14.sp,
                            )
                            Text(
                                text = "Login",
                                color = TechTeal,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        clearErrors()
                                        screenState = ScreenState.LOGIN
                                    }
                                    .padding(4.dp),
                            )
                        }

                        SignUpContent(
                            fullName = fullName,
                            onFullNameChange = { fullName = it; nameError = null },
                            nameError = nameError,
                            phoneNumber = phoneNumber,
                            onPhoneChange = { input ->
                                if (input.all { it.isDigit() }) {
                                    phoneNumber = input; phoneError = null
                                }
                            },
                            phoneError = phoneError,
                            email = emailAddress,
                            onEmailChange = { emailAddress = it; emailError = null },
                            emailError = emailError,
                            password = passwordInput,
                            onPasswordChange = { passwordInput = it; passwordError = null },
                            passwordError = passwordError,
                            isPasswordVisible = isPasswordVisible,
                            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                            isLoading = isLoading,
                            onSubmit = { submit() },
                            focusManager = focusManager,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

private fun parseServerError(errorBody: String?): String {
    if (errorBody == null) return "Something went wrong. Please try again."
    return try {
        val json = org.json.JSONObject(errorBody)
        json.optString("message", json.optString("error", "Something went wrong. Please try again."))
    } catch (_: Exception) {
        errorBody
    }
}
