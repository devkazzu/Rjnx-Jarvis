package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthProvider
import com.example.data.sync.SyncStatus
import com.example.ui.theme.*
import com.example.viewmodel.JarvisViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel,
    onOpenAuthDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val aiPersonality by viewModel.aiPersonality.collectAsState()
    val aiPersonalityTone by viewModel.aiPersonalityTone.collectAsState()
    val aiResponseVerbosity by viewModel.aiResponseVerbosity.collectAsState()
    val aiCustomDirective by viewModel.aiCustomDirective.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val isVoiceOutputEnabled by viewModel.isVoiceOutputEnabled.collectAsState()
    val isWakeWordEnabled by viewModel.isWakeWordEnabled.collectAsState()
    val pitch by viewModel.speechPitch.collectAsState()
    val rate by viewModel.speechRate.collectAsState()
    val accentColorName by viewModel.accentColor.collectAsState()

    val currentUser by viewModel.currentUser.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsState()

    var apiKeyInput by remember(customApiKey) { mutableStateOf(customApiKey) }
    var hideKey by remember { mutableStateOf(true) }
    var customDirInput by remember(aiCustomDirective) { mutableStateOf(aiCustomDirective) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var jsonInputText by remember { mutableStateOf("") }

    val themeOptions = listOf("Dark Futuristic", "Light Futuristic", "Cyberpunk Neon", "OLED Pitch Black")
    val toneOptions = listOf(
        "Classic Futuristic",
        "Formal & Professional",
        "Friendly & Empathetic",
        "Witty & Sarcastic",
        "Academic Tutor",
        "Tech Guru",
        "Motivational Coach"
    )
    val verbosityOptions = listOf("Concise", "Balanced", "Verbose & Detailed")
    val accentOptions = listOf("Cyan", "Magenta", "Purple", "Amber", "Emerald")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile & Authentication Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp, backgroundColor = DeepSpaceSurface)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("USER ACCOUNT & AUTH", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (currentUser.authProvider) {
                            AuthProvider.GOOGLE -> ElectricBlue.copy(alpha = 0.25f)
                            AuthProvider.EMAIL -> GlowingMagenta.copy(alpha = 0.25f)
                            AuthProvider.GUEST -> GlassSurfaceLight
                        }
                    ) {
                        Text(
                            text = currentUser.authProvider.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (currentUser.authProvider == AuthProvider.GOOGLE) ElectricBlue else GlowingMagenta)
                    ) {
                        Text(
                            text = currentUser.displayName.take(1).uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentUser.displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(currentUser.email, fontSize = 12.sp, color = TextSecondary)
                        Text("User ID: ${currentUser.userId.take(12)}...", fontSize = 10.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOpenAuthDialog,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Icon(
                            imageVector = if (currentUser.authProvider != AuthProvider.GUEST) Icons.Default.SwitchAccount else Icons.Default.Login,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (currentUser.authProvider != AuthProvider.GUEST) "ACCOUNT SETTINGS" else "SIGN IN / REGISTER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (currentUser.authProvider != AuthProvider.GUEST) {
                        OutlinedButton(
                            onClick = { viewModel.signOut() },
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // AI Assistant Personality Customizer Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = GlowingMagenta)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI PERSONALITY & DIRECTIVES", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Customize how RJNX Jarvis speaks, responds, and adapts to your workflow.", fontSize = 11.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(12.dp))

                Text("Personality Tone", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(toneOptions) { option ->
                        val isSelected = option == aiPersonalityTone
                        Box(
                            modifier = Modifier
                                .glassCard(
                                    cornerRadius = 14.dp,
                                    backgroundColor = if (isSelected) GlowingMagenta.copy(alpha = 0.28f) else GlassSurfaceDark
                                )
                                .clickable { viewModel.updatePersonalityTone(option) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(option, fontSize = 12.sp, color = if (isSelected) GlowingMagenta else TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Response Verbosity", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    verbosityOptions.forEach { v ->
                        val isSelected = v == aiResponseVerbosity
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .glassCard(
                                    cornerRadius = 14.dp,
                                    backgroundColor = if (isSelected) NeonCyan.copy(alpha = 0.25f) else GlassSurfaceDark
                                )
                                .clickable { viewModel.updateResponseVerbosity(v) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(v, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) NeonCyan else TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Custom System Directive", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = customDirInput,
                    onValueChange = { customDirInput = it },
                    placeholder = { Text("e.g. 'Always address me as Sir', 'Use bullet points'", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.updateCustomDirective(customDirInput)
                        Toast.makeText(context, "AI Directive Saved", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowingMagenta),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("SAVE DIRECTIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Cloud Backup & Firestore Sync Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CLOUD BACKUP & FIRESTORE SYNC", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    if (syncStatus is SyncStatus.Syncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NeonCyan, strokeWidth = 2.dp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Securely backup chat history and user profile to Firestore Cloud for multi-device access.", fontSize = 11.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto Cloud Sync", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Sync chat messages to cloud automatically", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = { viewModel.setAutoSync(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (lastSyncTimestamp > 0) {
                    val formattedTime = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(Date(lastSyncTimestamp))
                    Text("Last Cloud Sync: $formattedTime", fontSize = 11.sp, color = NeonCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (syncStatus is SyncStatus.Success) {
                    Text((syncStatus as SyncStatus.Success).message, fontSize = 11.sp, color = EmeraldGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (syncStatus is SyncStatus.Error) {
                    Text((syncStatus as SyncStatus.Error).errorMessage, fontSize = 11.sp, color = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.backupChatHistoryToCloud() },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("BACKUP TO CLOUD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.restoreChatHistoryFromCloud() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESTORE LOGS", fontSize = 11.sp, color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val json = viewModel.exportChatHistoryJson()
                            viewModel.copyToClipboard(json)
                            Toast.makeText(context, "Backup JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EXPORT JSON", fontSize = 10.sp, color = TextSecondary)
                    }

                    OutlinedButton(
                        onClick = { showImportJsonDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("IMPORT JSON", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Theme Settings Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("THEME & VISUAL MODE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text("Theme Style", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(themeOptions) { option ->
                        val isSelected = option == themeMode
                        Box(
                            modifier = Modifier
                                .glassCard(
                                    cornerRadius = 14.dp,
                                    backgroundColor = if (isSelected) NeonCyan.copy(alpha = 0.25f) else GlassSurfaceDark
                                )
                                .clickable { viewModel.updateTheme(option) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(option, fontSize = 12.sp, color = if (isSelected) NeonCyan else TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Accent Glow Color", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accentOptions) { accent ->
                        val isSelected = accent == accentColorName
                        Box(
                            modifier = Modifier
                                .glassCard(
                                    cornerRadius = 14.dp,
                                    backgroundColor = if (isSelected) GlowingMagenta.copy(alpha = 0.25f) else GlassSurfaceDark
                                )
                                .clickable { viewModel.updateAccentColor(accent) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(accent, fontSize = 12.sp, color = if (isSelected) GlowingMagenta else TextPrimary)
                        }
                    }
                }
            }
        }

        // Gemini API Key Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = CyberAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GEMINI API KEY CONFIG", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Default system key active. You can optionally override with a custom Gemini API Key.", fontSize = 11.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Custom API Key") },
                    visualTransformation = if (hideKey) PasswordVisualTransformation() else VisualTransformation.None,
                    trailingIcon = {
                        IconButton(onClick = { hideKey = !hideKey }) {
                            Icon(if (hideKey) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel.updateApiKey(apiKeyInput)
                        Toast.makeText(context, "API Key Saved", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber)
                ) {
                    Text("SAVE API KEY", color = VoidBackground, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Voice Engine Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = QuantumPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VOICE ASSISTANT CONFIG", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto Voice Readout", fontSize = 13.sp, color = TextPrimary)
                    Switch(
                        checked = isVoiceOutputEnabled,
                        onCheckedChange = { viewModel.updateVoiceOutput(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Wake Word ('Hey Jarvis')", fontSize = 13.sp, color = TextPrimary)
                    Switch(
                        checked = isWakeWordEnabled,
                        onCheckedChange = { viewModel.updateWakeWord(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = GlowingMagenta)
                    )
                }

                val isBgServiceEnabled by viewModel.isBackgroundServiceEnabled.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Background Assistant Service", fontSize = 13.sp, color = TextPrimary)
                        Text("Keep assistant active in background with notification", fontSize = 10.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isBgServiceEnabled,
                        onCheckedChange = { viewModel.toggleBackgroundService(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                val isFloatingButtonEnabled by viewModel.isFloatingButtonEnabled.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Floating Overlay Button", fontSize = 13.sp, color = TextPrimary)
                        Text("Show floating quick access widget on screen", fontSize = 10.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isFloatingButtonEnabled,
                        onCheckedChange = { viewModel.toggleFloatingOverlay(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberAmber)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Speech Pitch (${String.format("%.1f", pitch)})", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = pitch,
                    onValueChange = { viewModel.updateSpeechPitch(it) },
                    valueRange = 0.5f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )

                Text("Speech Rate (${String.format("%.1f", rate)})", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = rate,
                    onValueChange = { viewModel.updateSpeechRate(it) },
                    valueRange = 0.5f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = GlowingMagenta, activeTrackColor = GlowingMagenta)
                )
            }
        }

        // About RJNX Jarvis
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RJNX JARVIS OS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Text("Version 1.1.0 (Firebase Auth & Cloud Sync Enabled)", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Built with Kotlin, Jetpack Compose, Firebase Authentication, Firestore, Room, and Gemini 3.5 Flash.", fontSize = 11.sp, color = TextPrimary)
            }
        }
    }

    if (showImportJsonDialog) {
        AlertDialog(
            onDismissRequest = { showImportJsonDialog = false },
            title = { Text("Import Chat History JSON", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste JSON backup payload to import chat history into local Room database:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonInputText,
                        onValueChange = { jsonInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("[{\"sender\":\"USER\",\"content\":\"Hi\"}]", fontSize = 11.sp) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importChatHistoryFromJson(jsonInputText)
                        showImportJsonDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("IMPORT NOW", color = VoidBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportJsonDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}

