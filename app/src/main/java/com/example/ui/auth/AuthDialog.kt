package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.auth.AuthResult
import com.example.auth.FirebaseAuthManager
import com.example.ui.theme.CoralPink
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PurplePrimary
import kotlinx.coroutines.launch

enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
    FORGOT_PASSWORD
}

@Composable
fun AuthDialog(
    authManager: FirebaseAuthManager,
    onDismiss: () -> Unit,
    onAuthSuccess: (displayName: String, email: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val authResult by authManager.authResult.collectAsState()

    var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> {
                onAuthSuccess(
                    result.user.displayName ?: displayName.ifBlank { "Student" },
                    result.user.email ?: email
                )
                authManager.clearResult()
                onDismiss()
            }
            is AuthResult.Error -> {
                localError = result.errorMessage
            }
            else -> {}
        }
    }

    Dialog(
        onDismissRequest = {
            authManager.clearResult()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp)
                .testTag("firebase_auth_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Surface(
                    shape = CircleShape,
                    color = PurplePrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (authMode) {
                                AuthMode.SIGN_IN -> Icons.Default.Lock
                                AuthMode.SIGN_UP -> Icons.Default.PersonAdd
                                AuthMode.FORGOT_PASSWORD -> Icons.Default.LockReset
                            },
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when (authMode) {
                        AuthMode.SIGN_IN -> "Welcome Back"
                        AuthMode.SIGN_UP -> "Create Student Account"
                        AuthMode.FORGOT_PASSWORD -> "Reset Password"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = when (authMode) {
                        AuthMode.SIGN_IN -> "Sign in to sync your timetable and homework across devices"
                        AuthMode.SIGN_UP -> "Register for real-time Firebase backup and sync"
                        AuthMode.FORGOT_PASSWORD -> "Enter your email to receive a password reset link"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Tabs (Sign In / Sign Up)
                if (authMode != AuthMode.FORGOT_PASSWORD) {
                    TabRow(
                        selectedTabIndex = if (authMode == AuthMode.SIGN_IN) 0 else 1,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = PurplePrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Tab(
                            selected = authMode == AuthMode.SIGN_IN,
                            onClick = {
                                authMode = AuthMode.SIGN_IN
                                localError = null
                                authManager.clearResult()
                            },
                            text = { Text("Sign In", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("auth_tab_sign_in")
                        )
                        Tab(
                            selected = authMode == AuthMode.SIGN_UP,
                            onClick = {
                                authMode = AuthMode.SIGN_UP
                                localError = null
                                authManager.clearResult()
                            },
                            text = { Text("Sign Up", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("auth_tab_sign_up")
                        )
                    }
                }

                // Error Banner
                AnimatedVisibility(visible = localError != null) {
                    localError?.let { err ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CoralPink.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CoralPink, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(err, color = CoralPink, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Password Reset Sent Banner
                AnimatedVisibility(visible = authResult is AuthResult.PasswordResetSent) {
                    val sentState = authResult as? AuthResult.PasswordResetSent
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldGreen.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Password reset email sent to ${sentState?.email}. Check your inbox.",
                                color = EmeraldGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Fields based on Mode
                if (authMode == AuthMode.SIGN_UP) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full / Student Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_input_name")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        localError = null
                    },
                    label = { Text("Email Address") },
                    placeholder = { Text("student@school.edu") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = if (authMode == AuthMode.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onDone = {
                            focusManager.clearFocus()
                            if (authMode == AuthMode.FORGOT_PASSWORD) {
                                coroutineScope.launch { authManager.sendPasswordReset(email) }
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_input_email")
                )

                // Password Field
                if (authMode != AuthMode.FORGOT_PASSWORD) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            localError = null
                        },
                        label = { Text("Password") },
                        placeholder = { Text("At least 6 characters") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (authMode == AuthMode.SIGN_IN) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = {
                                focusManager.clearFocus()
                                if (authMode == AuthMode.SIGN_IN) {
                                    coroutineScope.launch { authManager.signIn(email, password) }
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_input_password")
                    )
                }

                // Confirm Password Field for Sign Up
                if (authMode == AuthMode.SIGN_UP) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            localError = null
                        },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (password != confirmPassword) {
                                    localError = "Passwords do not match."
                                } else {
                                    coroutineScope.launch { authManager.signUp(email, password, displayName) }
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_input_confirm_password")
                    )
                }

                // Forgot Password link for Sign In
                if (authMode == AuthMode.SIGN_IN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                authMode = AuthMode.FORGOT_PASSWORD
                                localError = null
                                authManager.clearResult()
                            }
                        ) {
                            Text("Forgot password?", fontSize = 12.sp, color = PurplePrimary)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Action Primary Button
                val isLoading = authResult is AuthResult.Loading
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        when (authMode) {
                            AuthMode.SIGN_IN -> {
                                coroutineScope.launch {
                                    authManager.signIn(email, password)
                                }
                            }
                            AuthMode.SIGN_UP -> {
                                if (password != confirmPassword) {
                                    localError = "Passwords do not match."
                                } else {
                                    coroutineScope.launch {
                                        authManager.signUp(email, password, displayName)
                                    }
                                }
                            }
                            AuthMode.FORGOT_PASSWORD -> {
                                coroutineScope.launch {
                                    authManager.sendPasswordReset(email)
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auth_action_submit_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = when (authMode) {
                                AuthMode.SIGN_IN -> "Sign In"
                                AuthMode.SIGN_UP -> "Create Account"
                                AuthMode.FORGOT_PASSWORD -> "Send Reset Link"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                if (authMode == AuthMode.FORGOT_PASSWORD) {
                    TextButton(
                        onClick = {
                            authMode = AuthMode.SIGN_IN
                            localError = null
                            authManager.clearResult()
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("← Back to Sign In", fontSize = 13.sp, color = PurplePrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Continue offline / cancel button
                TextButton(
                    onClick = {
                        authManager.clearResult()
                        onDismiss()
                    }
                ) {
                    Text("Close (Continue Offline)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
