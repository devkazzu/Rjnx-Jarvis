
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthState
import com.example.ui.theme.*

@Composable
fun AuthGateScreen(
    authState: AuthState,
    onEmailSignIn: (String, String) -> Unit,
    onEmailRegister: (String, String, String) -> Unit,
    onGoogle: () -> Unit,
    onGithub: () -> Unit,
    onPhoneStart: (String, () -> Unit) -> Unit,
    onPhoneVerify: (String) -> Unit
) {
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var phoneMode by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        message = when (authState) {
            is AuthState.Error -> authState.message
            else -> message
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF030712), Color(0xFF071827), Color(0xFF02040A)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.size(86.dp).clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF071D2D))
                    .border(1.dp, NeonCyan.copy(alpha = .65f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = NeonCyan, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("ANU THAPA", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("NEURAL ACCESS", fontSize = 11.sp, letterSpacing = 3.sp, color = NeonCyan)
            Spacer(Modifier.height(10.dp))
            Text(
                if (register) "Create your private Anu identity" else "Authenticate to activate Anu Core",
                color = TextSecondary, fontSize = 13.sp
            )

            Spacer(Modifier.height(24.dp))

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFF0A1420)).padding(4.dp)
            ) {
                AuthTab("LOGIN", !register) { register = false; phoneMode = false }
                AuthTab("REGISTER", register) { register = true; phoneMode = false }
            }

            Spacer(Modifier.height(18.dp))

            if (phoneMode) {
                OutlinedTextField(
                    phone, { phone = it }, Modifier.fillMaxWidth(),
                    label = { Text("Phone number (+91...)") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                if (otp.isNotBlank() || message == "OTP_SENT") {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        otp, { otp = it }, Modifier.fillMaxWidth(),
                        label = { Text("6-digit OTP") },
                        leadingIcon = { Icon(Icons.Default.Password, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
    onClick = {
        if (message == "OTP_SENT") onPhoneVerify(otp)
        else onPhoneStart(phone) { message = "OTP_SENT" }
    },
    modifier = Modifier.fillMaxWidth(),
    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Icon(Icons.Default.Phone, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (message == "OTP_SENT") "VERIFY OTP" else "SEND OTP", fontWeight = FontWeight.Bold)
                }
                TextButton({ phoneMode = false }) { Text("Use email instead", color = TextSecondary) }
            } else {
                if (register) {
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Display name") },
                        leadingIcon = { Icon(Icons.Default.Person, null) }, singleLine = true)
                    Spacer(Modifier.height(9.dp))
                }
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Gmail / Email") },
                    leadingIcon = { Icon(Icons.Default.Email, null) }, singleLine = true)
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true)

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (register) onEmailRegister(email, password, name)
                        else onEmailSignIn(email, password)
                    },
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(
                        containerColor = if (register) GlowingMagenta else ElectricBlue
                    )
                ) {
                    Icon(if (register) Icons.Default.PersonAdd else Icons.Default.Login, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (register) "CREATE ANU ID" else "ENTER ANU", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
) {
                HorizontalDivider(Modifier.weight(1f), color = TextSecondary.copy(alpha = .2f))
                Text("  OR  ", color = TextSecondary, fontSize = 10.sp)
                HorizontalDivider(Modifier.weight(1f), color = TextSecondary.copy(alpha = .2f))
            }
            Spacer(Modifier.height(12.dp))

            ProviderButton("G", "Continue with Google", onGoogle)
            Spacer(Modifier.height(8.dp))
            ProviderButton("GH", "Continue with GitHub", onGithub)
            Spacer(Modifier.height(8.dp))
            ProviderButton("☎", "Continue with phone number") { phoneMode = true; message = "" }

            if (message.isNotBlank() && message != "OTP_SENT") {
                Spacer(Modifier.height(12.dp))
                Text(message, color = Color(0xFFFF7A7A), fontSize = 11.sp)
            }

            Spacer(Modifier.weight(1f))
            Text("PRIVATE ACCESS • ANU CORE", color = TextSecondary, fontSize = 9.sp, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
private fun RowScope.AuthTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
            .background(if (selected) NeonCyan.copy(alpha = .18f) else Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (selected) NeonCyan else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ProviderButton(
    icon: String,
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp)
    ) {
        Text(
            icon,
            fontWeight = FontWeight.Black,
            color = NeonCyan
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text,
            color = TextSecondary
        )
    }
}
