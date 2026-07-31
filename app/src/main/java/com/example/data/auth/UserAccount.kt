package com.example.data.auth

data class UserAccount(
    val userId: String = "guest_user",
    val email: String = "guest@jarvis.ai",
    val displayName: String = "Jarvis User",
    val photoUrl: String = "",
    val authProvider: AuthProvider = AuthProvider.GUEST,
    val isEmailVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

enum class AuthProvider {
    EMAIL,
    GOOGLE,
    GUEST
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: UserAccount) : AuthState()
    data class Error(val message: String) : AuthState()
}
