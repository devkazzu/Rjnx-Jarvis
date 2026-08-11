package com.example.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class AuthRepository(private val context: Context) {

    private val firebaseAuth: FirebaseAuth by lazy {
    if (FirebaseApp.getApps(context).isEmpty()) {
        FirebaseApp.initializeApp(context)
    }
    FirebaseAuth.getInstance()
}

        if (FirebaseApp.getApps(context).isEmpty()) {
            null
        } else {
            FirebaseAuth.getInstance()
        }
    } catch (e: Exception) {
        Log.e("AuthRepository", "Firebase initialization failed", e)
        null
    }
}

    private val _currentUser = MutableStateFlow<UserAccount>(getInitialUser())
    val currentUser: StateFlow<UserAccount> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val prefs = context.getSharedPreferences("jarvis_auth_prefs", Context.MODE_PRIVATE)

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
                _currentUser.value = getInitialUser()
                _authState.value = AuthState.Idle
            }
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            if (email.isBlank() || pass.length < 6) {
                val err = "Enter a valid email and a password of at least 6 characters."
                _authState.value = AuthState.Error(err)
                return@withContext Result.failure(Exception(err))
            }
            val auth = firebaseAuth
             val result = auth.signInWithEmailAndPassword(email, pass).await()
            val fbUser = result.user ?: throw Exception("Authentication returned no user.")
            val user = UserAccount(
                userId = fbUser.uid,
                email = fbUser.email ?: email,
                displayName = fbUser.displayName ?: email.substringBefore("@"),
                photoUrl = fbUser.photoUrl?.toString() ?: "",
                authProvider = AuthProvider.EMAIL,
                isEmailVerified = fbUser.isEmailVerified
            )
            saveUserLocally(user)
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Sign in failed"
            _authState.value = AuthState.Error(msg)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, name: String): Result<UserAccount> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            if (email.isBlank() || pass.length < 6) {
                val err = "Enter a valid email and a password of at least 6 characters."
                _authState.value = AuthState.Error(err)
                return@withContext Result.failure(Exception(err))
            }
            val auth = firebaseAuth ?: throw Exception("Firebase Authentication is not configured.")
            val displayName = name.ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } }
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val fbUser = result.user ?: throw Exception("Account creation returned no user.")
            fbUser.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
            ).await()
            val user = UserAccount(
                userId = fbUser.uid,
                email = fbUser.email ?: email,
                displayName = displayName,
                authProvider = AuthProvider.EMAIL
            )
            saveUserLocally(user)
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Registration failed"
            _authState.value = AuthState.Error(msg)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String, userEmail: String? = null, userName: String? = null): Result<UserAccount> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Loading
        try {
            val auth = firebaseAuth ?: throw Exception("Firebase Authentication is not configured.")
            if (idToken.isBlank()) throw Exception("Google sign-in token is missing.")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val fbUser = result.user ?: throw Exception("Google authentication returned no user.")
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
            val msg = e.localizedMessage ?: "Google sign-in failed"
            _authState.value = AuthState.Error(msg)
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

    suspend fun signInWithGithub(activity: android.app.Activity): Result<UserAccount> = withContext(Dispatchers.Main) {
        _authState.value = AuthState.Loading
        try {
            val auth = firebaseAuth ?: throw Exception("Firebase Authentication is not configured.")
            val provider = com.google.firebase.auth.OAuthProvider.newBuilder("github.com").build()
            val result = auth.startActivityForSignInWithProvider(activity, provider).await()
            val fbUser = result.user ?: throw Exception("GitHub authentication returned no user.")
            val user = UserAccount(
                userId = fbUser.uid,
                email = fbUser.email ?: "",
                displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "GitHub User",
                photoUrl = fbUser.photoUrl?.toString() ?: "",
                authProvider = AuthProvider.GITHUB,
                isEmailVerified = true
            )
            saveUserLocally(user)
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "GitHub sign-in failed"
            _authState.value = AuthState.Error(msg)
            Result.failure(e)
        }
    }

    private var phoneVerificationId: String? = null

    fun startPhoneVerification(
        activity: android.app.Activity,
        phoneNumber: String,
        onCodeSent: () -> Unit
    ) {
        _authState.value = AuthState.Loading
        val auth = firebaseAuth
        if (auth == null) {
            _authState.value = AuthState.Error("Firebase Authentication is not configured.")
            return
        }
        val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    completePhoneCredential(credential)
                }
                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    _authState.value = AuthState.Error(e.localizedMessage ?: "Phone verification failed")
                }
                override fun onCodeSent(
                    verificationId: String,
                    token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
                ) {
                    phoneVerificationId = verificationId
                    _authState.value = AuthState.Idle
                    onCodeSent()
                }
            }).build()
        com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyPhoneCode(code: String) {
        val id = phoneVerificationId
        if (id.isNullOrBlank() || code.isBlank()) {
            _authState.value = AuthState.Error("Enter the OTP code.")
            return
        }
        completePhoneCredential(com.google.firebase.auth.PhoneAuthProvider.getCredential(id, code))
    }

    private fun completePhoneCredential(credential: com.google.firebase.auth.PhoneAuthCredential) {
        val auth = firebaseAuth ?: return
        _authState.value = AuthState.Loading
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                val fbUser = result.user ?: throw Exception("Phone authentication returned no user.")
                val user = UserAccount(
                    userId = fbUser.uid,
                    email = fbUser.email ?: "${fbUser.phoneNumber ?: "phone"}@anu.local",
                    displayName = fbUser.displayName ?: fbUser.phoneNumber ?: "Anu User",
                    authProvider = AuthProvider.PHONE,
                    isEmailVerified = true
                )
                saveUserLocally(user)
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "OTP verification failed")
            }
        }
    }

    fun signOut() {
        runCatching { firebaseAuth?.signOut() }
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
