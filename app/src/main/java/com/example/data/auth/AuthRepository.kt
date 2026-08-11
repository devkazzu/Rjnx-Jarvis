package com.example.data.auth

import android.app.Activity
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AuthRepository(private val context: Context) {

    private val firebaseAuth: FirebaseAuth by lazy {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        FirebaseAuth.getInstance()
    }

    private val _currentUser = MutableStateFlow<UserAccount>(getInitialUser())
    val currentUser: StateFlow<UserAccount> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val prefs =
        context.getSharedPreferences("jarvis_auth_prefs", Context.MODE_PRIVATE)

    private var phoneVerificationId: String? = null

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        val fbUser = firebaseAuth.currentUser

        if (fbUser != null) {
            val provider = when {
                fbUser.providerData.any { it.providerId == "google.com" } -> AuthProvider.GOOGLE
                fbUser.providerData.any { it.providerId == "github.com" } -> AuthProvider.GITHUB
                fbUser.providerData.any { it.providerId == "phone" } -> AuthProvider.PHONE
                else -> AuthProvider.EMAIL
            }

            val user = UserAccount(
                userId = fbUser.uid,
                email = fbUser.email ?: "user@jarvis.ai",
                displayName = fbUser.displayName
                    ?: fbUser.email?.substringBefore("@")
                    ?: fbUser.phoneNumber
                    ?: "Jarvis Agent",
                photoUrl = fbUser.photoUrl?.toString() ?: "",
                authProvider = provider,
                isEmailVerified = fbUser.isEmailVerified,
                createdAt = fbUser.metadata?.creationTimestamp
                    ?: System.currentTimeMillis(),
                lastLoginAt = fbUser.metadata?.lastSignInTimestamp
                    ?: System.currentTimeMillis()
            )

            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
            return
        }

        val savedUserId = prefs.getString("user_id", null)

        if (savedUserId != null) {
            val user = UserAccount(
                userId = savedUserId,
                email = prefs.getString("email", "agent@jarvis.ai")
                    ?: "agent@jarvis.ai",
                displayName = prefs.getString(
                    "display_name",
                    "Jarvis Operative"
                ) ?: "Jarvis Operative",
                photoUrl = prefs.getString("photo_url", "") ?: "",
                authProvider = runCatching {
                    AuthProvider.valueOf(
                        prefs.getString("provider", "EMAIL") ?: "EMAIL"
                    )
                }.getOrDefault(AuthProvider.EMAIL),
                createdAt = prefs.getLong(
                    "created_at",
                    System.currentTimeMillis()
                ),
                lastLoginAt = System.currentTimeMillis()
            )

            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
        } else {
            _currentUser.value = getInitialUser()
            _authState.value = AuthState.Idle
        }
    }

    suspend fun signInWithEmail(
        email: String,
        pass: String
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading

        try {
            if (email.isBlank() || pass.length < 6) {
                val error = "Enter a valid email and a password of at least 6 characters."
                _authState.value = AuthState.Error(error)
                return@withContext Result.failure(Exception(error))
            }

            val result = firebaseAuth
                .signInWithEmailAndPassword(email, pass)
                .await()

            val fbUser = result.user
                ?: throw Exception("Authentication returned no user.")

            val user = UserAccount(
                userId = fbUser.uid,
                email = fbUser.email ?: email,
                displayName = fbUser.displayName
                    ?: email.substringBefore("@"),
                photoUrl = fbUser.photoUrl?.toString() ?: "",
                authProvider = AuthProvider.EMAIL,
                isEmailVerified = fbUser.isEmailVerified
            )

            saveUserLocally(user)
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)

            Result.success(user)
        } catch (e: Exception) {
            val message = e.localizedMessage ?: "Sign in failed"
            _authState.value = AuthState.Error(message)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        pass: String,
        name: String
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading

        try {
            if (email.isBlank() || pass.length < 6) {
                val error = "Enter a valid email and a password of at least 6 characters."
                _authState.value = AuthState.Error(error)
                return@withContext Result.failure(Exception(error))
            }

            val displayName = name.ifBlank {
                email.substringBefore("@")
                    .replaceFirstChar { it.uppercase() }
            }

            val result = firebaseAuth
                .createUserWithEmailAndPassword(email, pass)
                .await()

            val fbUser = result.user
                ?: throw Exception("Account creation returned no user.")

            fbUser.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
            ).await()

            val user = UserAccount(
                userId = fbUser.uid,
                email = fbUser.email ?: email,
                displayName = displayName,
                photoUrl = fbUser.photoUrl?.toString() ?: "",
                authProvider = AuthProvider.EMAIL,
                isEmailVerified = fbUser.isEmailVerified
            )

            saveUserLocally(user)
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)

            Result.success(user)
        } catch (e: Exception) {
            val message = e.localizedMessage ?: "Registration failed"
            _authState.value = AuthState.Error(message)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleCredential(
        idToken: String,
        userEmail: String? = null,
        userName: String? = null
    ): Result<UserAccount> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading

        try {
            if (idToken.isBlank()) {
                throw Exception("Google sign-in token is missing.")
            }

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()

            val fbUser = result.user
                ?: throw Exception("Google authentication returned no user.")

            val user = UserAccount(
                userId = fbUser.uid,
                email = fbUser.email ?: userEmail ?: "",
                displayName = fbUser.displayName ?: userName ?: "Anu User",
                photoUrl = fbUser.photoUrl?.toString() ?: "",
                authProvider = AuthProvider.GOOGLE,
                isEmailVerified = true
            )

            saveUserLocally(user)
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)

            Result.success(user)
        } catch (e: Exception) {
            val message = e.localizedMessage ?: "Google sign-in failed"
            _authState.value = AuthState.Error(message)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (email.isBlank()) {
                    throw Exception("Enter your email address.")
                }

                firebaseAuth.sendPasswordResetEmail(email).await()
                Result.success(true)
            } catch (e: Exception) {
                val message = e.localizedMessage ?: "Password reset failed"
                _authState.value = AuthState.Error(message)
                Result.failure(e)
            }
        }

    suspend fun signInWithGithub(
        activity: Activity
    ): Result<UserAccount> = withContext(Dispatchers.Main) {
        _authState.value = AuthState.Loading

        try {
            val provider =
                com.google.firebase.auth.OAuthProvider
                    .newBuilder("github.com")
                    .build()

            val result = firebaseAuth
                .startActivityForSignInWithProvider(activity, provider)
                .await()

            val fbUser = result.user
                ?: throw Exception("GitHub authentication returned no user.")

            val user = UserAccount(
                userId = fbUser.uid,
                email = fbUser.email ?: "",
                displayName = fbUser.displayName
                    ?: fbUser.email?.substringBefore("@")
                    ?: "GitHub User",
                photoUrl = fbUser.photoUrl?.toString() ?: "",
                authProvider = AuthProvider.GITHUB,
                isEmailVerified = true
            )

            saveUserLocally(user)
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)

            Result.success(user)
        } catch (e: Exception) {
            val message = e.localizedMessage ?: "GitHub sign-in failed"
            _authState.value = AuthState.Error(message)
            Result.failure(e)
        }
    }

    fun startPhoneVerification(
        activity: Activity,
        phoneNumber: String,
        onCodeSent: () -> Unit
    ) {
        if (phoneNumber.isBlank()) {
            _authState.value = AuthState.Error("Enter a phone number.")
            return
        }

        _authState.value = AuthState.Loading

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(
                object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    override fun onVerificationCompleted(
                        credential: com.google.firebase.auth.PhoneAuthCredential
                    ) {
                        completePhoneCredential(credential)
                    }

                    override fun onVerificationFailed(
                        e: com.google.firebase.FirebaseException
                    ) {
                        _authState.value = AuthState.Error(
                            e.localizedMessage ?: "Phone verification failed"
                        )
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        phoneVerificationId = verificationId
                        _authState.value = AuthState.Idle
                        onCodeSent()
                    }
                }
            )
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyPhoneCode(code: String) {
        val id = phoneVerificationId

        if (id.isNullOrBlank() || code.isBlank()) {
            _authState.value = AuthState.Error("Enter the OTP code.")
            return
        }

        val credential =
            PhoneAuthProvider.getCredential(id, code)

        completePhoneCredential(credential)
    }

    private fun completePhoneCredential(
        credential: com.google.firebase.auth.PhoneAuthCredential
    ) {
        _authState.value = AuthState.Loading

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseAuth
                    .signInWithCredential(credential)
                    .await()

                val fbUser = result.user
                    ?: throw Exception("Phone authentication returned no user.")

                val user = UserAccount(
                    userId = fbUser.uid,
                    email = fbUser.email
                        ?: "${fbUser.phoneNumber ?: "phone"}@anu.local",
                    displayName = fbUser.displayName
                        ?: fbUser.phoneNumber
                        ?: "Anu User",
                    photoUrl = fbUser.photoUrl?.toString() ?: "",
                    authProvider = AuthProvider.PHONE,
                    isEmailVerified = true
                )

                saveUserLocally(user)
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    e.localizedMessage ?: "OTP verification failed"
                )
            }
        }
    }

    fun signOut() {
        runCatching {
            firebaseAuth.signOut()
        }

        prefs.edit().clear().apply()
        _currentUser.value = getInitialUser()
        _authState.value = AuthState.Idle
    }

    private fun saveUserLocally(user: UserAccount) {
        prefs.edit()
            .putString("user_id", user.userId)
            .putString("email", user.email)
            .putString("display_name", user.displayName)
            .putString("photo_url", user.photoUrl)
            .putString("provider", user.authProvider.name)
            .putLong("created_at", user.createdAt)
            .apply()
    }

    private fun getInitialUser(): UserAccount {
        return UserAccount(
            userId = "guest_user",
            email = "guest@jarvis.ai",
            displayName = "Guest Commander",
            authProvider = AuthProvider.GUEST
        )
    }
}
