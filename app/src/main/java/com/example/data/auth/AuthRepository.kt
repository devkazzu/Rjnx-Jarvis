package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {

    private val firebaseAuth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    private val _currentUser = MutableStateFlow<UserAccount>(getInitialUser())
    val currentUser: StateFlow<UserAccount> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val prefs = context.getSharedPreferences("jarvis_auth_prefs", Context.MODE_PRIVATE)
    private var phoneVerificationId: String? = null

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        val fbUser = firebaseAuth?.currentUser
        if (fbUser != null) {
            val user = UserAccount(
                userId = fbUser.uid,
                email = fbUser.email ?: "user@jarvis.ai",
                displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "Jarvis Agent",
                photoUrl = fbUser.photoUrl?.toString() ?: "",
                authProvider = if (fbUser.providerData.any { it.providerId == "google.com" }) AuthProvider.GOOGLE else AuthProvider.EMAIL,
                isEmailVerified = fbUser.isEmailVerified,
                createdAt = fbUser.metadata?.creationTimestamp ?: System.currentTimeMillis(),
                lastLoginAt = fbUser.metadata?.lastSignInTimestamp ?: System.currentTimeMillis()
            )
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
        } else {
            val savedUserId = prefs.getString("user_id", null)
            if (savedUserId != null) {
                val user = UserAccount(
                    userId = savedUserId,
                    email = prefs.getString("email", "agent@jarvis.ai") ?: "agent@jarvis.ai",
                    displayName = prefs.getString("display_name", "Jarvis Operative") ?: "Jarvis Operative",
                    photoUrl = prefs.getString("photo_url", "") ?: "",
                    authProvider = try { AuthProvider.valueOf(prefs.getString("provider", "EMAIL") ?: "EMAIL") } catch (e: Exception) { AuthProvider.EMAIL },
                    createdAt = prefs.getLong("created_at", System.currentTimeMillis()),
                    lastLoginAt = System.currentTimeMillis()
                )
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
            } else {
                val guest = UserAccount(userId = "guest_user", email = "guest@jarvis.ai", displayName = "Guest Commander", authProvider = AuthProvider.GUEST)
                _currentUser.value = guest
                _authState.value = AuthState.Authenticated(guest)
            }
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            if (email.isBlank() || pass.length < 6) {
                val err = "Email cannot be empty and password must be at least 6 characters."
                _authState.value = AuthState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val auth = firebaseAuth
            if (auth == null) {
                val err = "Firebase Authentication is not configured."
                _authState.value = AuthState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            // Email/password login MUST be verified by Firebase.
            // Never fall back to local authentication here, otherwise any password
            // would be accepted after Firebase rejects the credentials.
            val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val fbUser = result.user
                ?: throw Exception("Authentication returned no user.")

            val user = UserAccount(
                userId = fbUser.uid,
                email = fbUser.email ?: email.trim(),
                displayName = fbUser.displayName ?: email.substringBefore("@"),
                photoUrl = fbUser.photoUrl?.toString() ?: "",
                authProvider = AuthProvider.EMAIL,
                isEmailVerified = fbUser.isEmailVerified,
                createdAt = fbUser.metadata?.creationTimestamp ?: System.currentTimeMillis(),
                lastLoginAt = fbUser.metadata?.lastSignInTimestamp ?: System.currentTimeMillis()
            )

            saveUserLocally(user)
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.w("AuthRepository", "Firebase email sign-in failed", e)
            val msg = e.localizedMessage ?: "Invalid email or password."
            _authState.value = AuthState.Error(msg)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, name: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            if (email.isBlank() || pass.length < 6) {
                val err = "Email cannot be empty and password must be at least 6 characters."
                _authState.value = AuthState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val displayName = name.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } }

            if (firebaseAuth != null) {
                try {
                    val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
                    val fbUser = result.user
                    if (fbUser != null) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build()
                        fbUser.updateProfile(profileUpdates).await()

                        val user = UserAccount(
                            userId = fbUser.uid,
                            email = fbUser.email ?: email,
                            displayName = displayName,
                            authProvider = AuthProvider.EMAIL
                        )
                        saveUserLocally(user)
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                        return@withContext Result.success(user)
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Firebase sign up failed: ${e.message}. Using fallback account creation.")
                }
            }

            val userId = "usr_" + email.hashCode().toString().takeLast(8)
            val user = UserAccount(
                userId = userId,
                email = email,
                displayName = displayName,
                authProvider = AuthProvider.EMAIL
            )
            saveUserLocally(user)
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Account registration failed"
            _authState.value = AuthState.Error(msg)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String, userEmail: String? = null, userName: String? = null): Result<UserAccount> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            if (firebaseAuth != null && idToken.isNotBlank()) {
                try {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    val result = firebaseAuth.signInWithCredential(credential).await()
                    val fbUser = result.user
                    if (fbUser != null) {
                        val user = UserAccount(
                            userId = fbUser.uid,
                            email = fbUser.email ?: userEmail ?: "google.user@jarvis.ai",
                            displayName = fbUser.displayName ?: userName ?: "Google Commander",
                            photoUrl = fbUser.photoUrl?.toString() ?: "",
                            authProvider = AuthProvider.GOOGLE,
                            isEmailVerified = true
                        )
                        saveUserLocally(user)
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                        return@withContext Result.success(user)
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Firebase Google credential sign in error: ${e.message}")
                }
            }

            // Google Sign In fallback
            val email = userEmail ?: "google.commander@jarvis.ai"
            val name = userName ?: "Google Commander"
            val userId = "goog_" + email.hashCode().toString().takeLast(8)
            val user = UserAccount(
                userId = userId,
                email = email,
                displayName = name,
                photoUrl = "https://lh3.googleusercontent.com/a/default-user",
                authProvider = AuthProvider.GOOGLE,
                isEmailVerified = true
            )
            saveUserLocally(user)
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Google sign in failed"
            _authState.value = AuthState.Error(msg)
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

        val auth = firebaseAuth
        if (auth == null) {
            _authState.value = AuthState.Error("Firebase Authentication is not configured.")
            return
        }

        _authState.value = AuthState.Loading

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(
                    credential: com.google.firebase.auth.PhoneAuthCredential
                ) {
                    completePhoneCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
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
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyPhoneCode(code: String) {
        val verificationId = phoneVerificationId
        if (verificationId.isNullOrBlank() || code.isBlank()) {
            _authState.value = AuthState.Error("Enter the OTP code.")
            return
        }
        completePhoneCredential(
            PhoneAuthProvider.getCredential(verificationId, code)
        )
    }

    private fun completePhoneCredential(
        credential: com.google.firebase.auth.PhoneAuthCredential
    ) {
        val auth = firebaseAuth
        if (auth == null) {
            _authState.value = AuthState.Error("Firebase Authentication is not configured.")
            return
        }

        _authState.value = AuthState.Loading
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                val fbUser = result.user
                    ?: throw Exception("Phone authentication returned no user.")

                val user = UserAccount(
                    userId = fbUser.uid,
                    email = fbUser.email ?: "${fbUser.phoneNumber ?: "phone"}@anu.local",
                    displayName = fbUser.displayName ?: fbUser.phoneNumber ?: "Anu User",
                    photoUrl = fbUser.photoUrl?.toString() ?: "",
                    authProvider = AuthProvider.EMAIL,
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

    suspend fun signInWithGithub(activity: Activity): Result<UserAccount> =
        withContext(Dispatchers.Main) {
            _authState.value = AuthState.Loading
            val auth = firebaseAuth
            if (auth == null) {
                val error = "Firebase Authentication is not configured."
                _authState.value = AuthState.Error(error)
                return@withContext Result.failure(Exception(error))
            }

            try {
                val provider = OAuthProvider.newBuilder("github.com").build()
                val result = auth.startActivityForSignInWithProvider(activity, provider).await()
                val fbUser = result.user
                    ?: throw Exception("GitHub authentication returned no user.")

                val user = UserAccount(
                    userId = fbUser.uid,
                    email = fbUser.email ?: "",
                    displayName = fbUser.displayName
                        ?: fbUser.email?.substringBefore("@")
                        ?: "GitHub User",
                    photoUrl = fbUser.photoUrl?.toString() ?: "",
                    authProvider = AuthProvider.EMAIL,
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

    suspend fun sendPasswordReset(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (firebaseAuth != null) {
                runCatching { firebaseAuth.sendPasswordResetEmail(email).await() }
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signInAsGuest() {
        val guest = UserAccount(
            userId = "guest_user",
            email = "guest@jarvis.ai",
            displayName = "Guest Commander",
            authProvider = AuthProvider.GUEST
        )
        saveUserLocally(guest)
        _currentUser.value = guest
        _authState.value = AuthState.Authenticated(guest)
    }

    fun signOut() {
        runCatching { firebaseAuth?.signOut() }
        prefs.edit().clear().apply()
        signInAsGuest()
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
