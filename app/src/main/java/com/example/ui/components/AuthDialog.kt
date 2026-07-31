package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.auth.AuthProvider
import com.example.data.auth.AuthState
import com.example.data.auth.UserAccount
import com.example.ui.theme.*

@Composable
fun AuthDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    currentUser: UserAccount,
    authState: AuthState,
    onSignInEmail: (String, String) -> Unit,
    onSignUpEmail: (String, String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onGuestSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onResetPassword: (String) -> Unit
) {
    if (!isOpen) return

    var isSignUpTab by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var hidePassword by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            statusMessage = authState.message
        } else if (authState is AuthState.Authenticated) {
            statusMessage = "Authenticated as ${authState.user.displayName}"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 24.dp, backgroundColor = DeepSpaceSurface)
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Auth",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentUser.authProvider != AuthProvider.GUEST) "COMMANDER ACCOUNT" else "AUTHENTICATE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GlassSurfaceLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (currentUser.authProvider != AuthProvider.GUEST) {
                    // Logged in User Badge view
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 16.dp, backgroundColor = GlassSurfaceDark)
                            .padding(16.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (currentUser.authProvider == AuthProvider.GOOGLE) ElectricBlue else GlowingMagenta)
                        ) {
                            Text(
                                text = currentUser.displayName.take(1).uppercase(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = currentUser.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = currentUser.email,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentUser.authProvider == AuthProvider.GOOGLE) ElectricBlue.copy(alpha = 0.2f) else QuantumPurple.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (currentUser.authProvider == AuthProvider.GOOGLE) Icons.Default.GMobiledata else Icons.Default.Email,
                                    contentDescription = null,
                                    tint = if (currentUser.authProvider == AuthProvider.GOOGLE) NeonCyan else GlowingMagenta,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentUser.authProvider == AuthProvider.GOOGLE) "Google Verified Account" else "Email Auth Account",
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onSignOut()
                                statusMessage = "Signed out successfully."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SIGN OUT", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Auth Forms (Sign In / Sign Up)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassSurfaceDark)
                            .padding(4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isSignUpTab) NeonCyan.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { isSignUpTab = false }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                "SIGN IN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isSignUpTab) NeonCyan else TextSecondary
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSignUpTab) GlowingMagenta.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { isSignUpTab = true }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                "REGISTER",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSignUpTab) GlowingMagenta else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isSignUpTab) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Display Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password (6+ chars)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { hidePassword = !hidePassword }) {
                                Icon(if (hidePassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                            }
                        },
                        visualTransformation = if (hidePassword) PasswordVisualTransformation() else VisualTransformation.None,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (isSignUpTab) {
                                onSignUpEmail(emailInput, passwordInput, nameInput)
                            } else {
                                onSignInEmail(emailInput, passwordInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSignUpTab) GlowingMagenta else ElectricBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(if (isSignUpTab) Icons.Default.PersonAdd else Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isSignUpTab) "CREATE ACCOUNT" else "SIGN IN", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Google Sign-In Button
                    OutlinedButton(
                        onClick = onGoogleSignIn,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Google", tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign in with Google", color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextTextButton(
                            text = "Forgot password?",
                            onClick = {
                                if (emailInput.isNotBlank()) {
                                    onResetPassword(emailInput)
                                    statusMessage = "Reset link sent to $emailInput"
                                } else {
                                    statusMessage = "Enter email first to reset password."
                                }
                            }
                        )

                        TextTextButton(
                            text = "Continue as Guest",
                            onClick = {
                                onGuestSignIn()
                                onDismiss()
                            }
                        )
                    }
                }

                if (statusMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 11.sp,
                        color = if (statusMessage.contains("failed") || statusMessage.contains("error") || statusMessage.contains("Enter email")) Color(0xFFF87171) else NeonCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun TextTextButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    )
}
