package com.example.auth

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class AuthResult {
    object Idle : AuthResult()
    object Loading : AuthResult()
    data class Success(val user: FirebaseUser, val message: String) : AuthResult()
    data class Error(val errorMessage: String) : AuthResult()
    data class PasswordResetSent(val email: String) : AuthResult()
}

data class AuthUserState(
    val user: FirebaseUser? = null,
    val isLoggedIn: Boolean = false,
    val email: String = "",
    val displayName: String = "",
    val uid: String = "",
    val isEmailVerified: Boolean = false
)

class FirebaseAuthManager private constructor(context: Context) {

    private val auth: FirebaseAuth by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            FirebaseAuth.getInstance()
        }
    }

    private val _userState = MutableStateFlow(AuthUserState())
    val userState: StateFlow<AuthUserState> = _userState.asStateFlow()

    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult.asStateFlow()

    init {
        try {
            auth.addAuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    _userState.value = AuthUserState(
                        user = currentUser,
                        isLoggedIn = true,
                        email = currentUser.email ?: "",
                        displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Student",
                        uid = currentUser.uid,
                        isEmailVerified = currentUser.isEmailVerified
                    )
                } else {
                    _userState.value = AuthUserState(
                        user = null,
                        isLoggedIn = false
                    )
                }
            }
        } catch (e: Exception) {
            // Graceful fallback
        }
    }

    fun clearResult() {
        _authResult.value = AuthResult.Idle
    }

    suspend fun signIn(email: String, pass: String): Result<FirebaseUser> {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()

        if (trimmedEmail.isBlank()) {
            val err = "Please enter your email address."
            _authResult.value = AuthResult.Error(err)
            return Result.failure(IllegalArgumentException(err))
        }
        if (trimmedPass.isBlank()) {
            val err = "Please enter your password."
            _authResult.value = AuthResult.Error(err)
            return Result.failure(IllegalArgumentException(err))
        }

        _authResult.value = AuthResult.Loading
        return try {
            val authResult = awaitTask(auth.signInWithEmailAndPassword(trimmedEmail, trimmedPass))
            val user = authResult.user ?: throw IllegalStateException("User not found after sign in")
            _authResult.value = AuthResult.Success(user, "Welcome back, ${user.displayName ?: user.email ?: "Student"}!")
            Result.success(user)
        } catch (e: Exception) {
            val friendlyMsg = parseAuthError(e)
            _authResult.value = AuthResult.Error(friendlyMsg)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    suspend fun signUp(email: String, pass: String, displayName: String): Result<FirebaseUser> {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        val trimmedName = displayName.trim()

        if (trimmedEmail.isBlank()) {
            val err = "Please enter an email address."
            _authResult.value = AuthResult.Error(err)
            return Result.failure(IllegalArgumentException(err))
        }
        if (trimmedPass.length < 6) {
            val err = "Password must be at least 6 characters long."
            _authResult.value = AuthResult.Error(err)
            return Result.failure(IllegalArgumentException(err))
        }

        _authResult.value = AuthResult.Loading
        return try {
            val authResult = awaitTask(auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPass))
            val user = authResult.user ?: throw IllegalStateException("Registration completed but user is null")

            if (trimmedName.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmedName)
                    .build()
                try {
                    awaitTask(user.updateProfile(profileUpdates))
                } catch (ignored: Exception) {
                    // Profile update failed, but user was created
                }
            }

            _authResult.value = AuthResult.Success(user, "Account created successfully for ${trimmedName.ifBlank { trimmedEmail }}!")
            Result.success(user)
        } catch (e: Exception) {
            val friendlyMsg = parseAuthError(e)
            _authResult.value = AuthResult.Error(friendlyMsg)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            val err = "Please enter your email to receive a password reset link."
            _authResult.value = AuthResult.Error(err)
            return Result.failure(IllegalArgumentException(err))
        }

        _authResult.value = AuthResult.Loading
        return try {
            awaitTask(auth.sendPasswordResetEmail(trimmedEmail))
            _authResult.value = AuthResult.PasswordResetSent(trimmedEmail)
            Result.success(Unit)
        } catch (e: Exception) {
            val friendlyMsg = parseAuthError(e)
            _authResult.value = AuthResult.Error(friendlyMsg)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    suspend fun signInWithGoogleCredential(credential: AuthCredential): Result<FirebaseUser> {
        _authResult.value = AuthResult.Loading
        return try {
            val authResult = awaitTask(auth.signInWithCredential(credential))
            val user = authResult.user ?: throw IllegalStateException("User not found after Google Sign-In")
            _authResult.value = AuthResult.Success(user, "Welcome, ${user.displayName ?: user.email ?: "Student"}!")
            Result.success(user)
        } catch (e: Exception) {
            val friendlyMsg = parseAuthError(e)
            _authResult.value = AuthResult.Error(friendlyMsg)
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return signInWithGoogleCredential(credential)
    }

    fun signOut() {
        try {
            auth.signOut()
            _authResult.value = AuthResult.Idle
        } catch (e: Exception) {
            // Handled
        }
    }

    private fun parseAuthError(e: Exception): String {
        return when (e) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
            is FirebaseAuthInvalidCredentialsException -> "Invalid credentials or malformed email address."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists. Please sign in instead."
            is FirebaseAuthInvalidUserException -> "No account found with this email. Please sign up first."
            is FirebaseAuthException -> when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
                "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again."
                "ERROR_USER_NOT_FOUND" -> "No user found with this email."
                "ERROR_USER_DISABLED" -> "This account has been disabled."
                "ERROR_TOO_MANY_REQUESTS" -> "Too many failed attempts. Please try again later."
                "ERROR_OPERATION_NOT_ALLOWED" -> "Email/password sign-in is not enabled in Firebase Console."
                else -> e.localizedMessage ?: "Authentication failed."
            }
            else -> {
                val msg = e.localizedMessage ?: "Authentication request failed."
                if (msg.contains("network", ignoreCase = true)) {
                    "Network error. Please check your internet connection and try again."
                } else {
                    msg
                }
            }
        }
    }

    private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { cont ->
        task.addOnCompleteListener { completedTask ->
            if (completedTask.isSuccessful) {
                cont.resume(completedTask.result)
            } else {
                val ex = completedTask.exception ?: Exception("Firebase task failed")
                cont.resumeWithException(ex)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: FirebaseAuthManager? = null

        fun getInstance(context: Context): FirebaseAuthManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
