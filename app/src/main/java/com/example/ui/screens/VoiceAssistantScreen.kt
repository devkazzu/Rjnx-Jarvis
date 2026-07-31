package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.JarvisViewModel

@Composable
fun VoiceAssistantScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val audioLevel by viewModel.voiceManager.audioWaveLevel.collectAsState()
    val recognizedText by viewModel.voiceManager.recognizedText.collectAsState()
    val lastResponse by viewModel.lastVoiceResponse.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    val quickCommands = listOf(
        "Hey Jarvis, show weather",
        "Set alarm for 7:00 AM",
        "Solve math doubt: 2x + 10 = 30",
        "Start 25 min study timer",
        "Add todo: Complete Android assignment",
        "Open Calculator"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Status HUD Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 16.dp)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isListening || isSpeaking) NeonCyan else EmeraldGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isListening) "LISTENING TO VOICE..." else if (isSpeaking) "JARVIS SPEAKING..." else if (isGenerating) "AI THINKING..." else "WAKE WORD ACTIVE ('Hey Jarvis')",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
                Text(text = "REST API v3.5", fontSize = 10.sp, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Animated Voice Orb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            JarvisVoiceOrb(
                isListening = isListening,
                isSpeaking = isSpeaking,
                audioLevel = audioLevel,
                size = 220.dp
            )
        }

        // Live Audio Waveform
        AudioWaveformVisualizer(
            isListening = isListening,
            isSpeaking = isSpeaking,
            audioLevel = audioLevel,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        // Live Voice Recognized / State Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (recognizedText.isNotBlank()) {
                    Text(
                        text = "\"$recognizedText\"",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = lastResponse,
                        fontSize = 14.sp,
                        color = TextPrimary.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Tap-to-Talk HUD Trigger
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    if (isSpeaking) {
                        viewModel.voiceManager.stopSpeaking()
                    } else if (isListening) {
                        viewModel.voiceManager.stopListening()
                    } else {
                        viewModel.voiceManager.startListening()
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening || isSpeaking) GlowingMagenta else NeonCyan
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.VolumeUp else if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Voice Trigger",
                    tint = VoidBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSpeaking) "STOP VOICE" else if (isListening) "STOP LISTENING" else "TAP TO SPEAK",
                    fontWeight = FontWeight.Bold,
                    color = VoidBackground,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Command Chips
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "QUICK VOICE COMMANDS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickCommands) { cmd ->
                    Box(
                        modifier = Modifier
                            .glassCard(cornerRadius = 16.dp, backgroundColor = GlassSurfaceDark)
                            .clickable { viewModel.sendChatMessage(cmd, isVoice = true) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(text = cmd, fontSize = 12.sp, color = NeonCyan)
                    }
                }
            }
        }
    }
}
