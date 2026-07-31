package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthProvider
import com.example.ui.theme.*

@Composable
fun JarvisHeader(
    title: String,
    subtitle: String,
    isListening: Boolean,
    isSpeaking: Boolean,
    onVoiceClick: () -> Unit,
    userName: String = "Guest",
    userProvider: AuthProvider = AuthProvider.GUEST,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Jarvis Logo",
                    tint = NeonCyan,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // User Profile Avatar Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (userProvider) {
                            AuthProvider.GOOGLE -> ElectricBlue.copy(alpha = 0.35f)
                            AuthProvider.EMAIL -> GlowingMagenta.copy(alpha = 0.35f)
                            AuthProvider.GUEST -> GlassSurfaceDark
                        }
                    )
                    .clickable { onProfileClick() }
            ) {
                if (userProvider != AuthProvider.GUEST && userName.isNotBlank()) {
                    Text(
                        text = userName.take(1).uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Voice Orb Quick Action Button
            IconButton(
                onClick = onVoiceClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isListening || isSpeaking) GlowingMagenta.copy(alpha = 0.25f)
                        else GlassSurfaceDark
                    )
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.Default.Mic,
                    contentDescription = "Voice Control",
                    tint = if (isListening) NeonCyan else if (isSpeaking) GlowingMagenta else Color.White
                )
            }
        }
    }
}

