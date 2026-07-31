package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import com.example.viewmodel.JarvisViewModel

@Composable
fun AssistantStatusScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isWakeWordEnabled by viewModel.isWakeWordEnabled.collectAsState()
    val isBgServiceEnabled by viewModel.isBackgroundServiceEnabled.collectAsState()
    val isBgServiceRunning by viewModel.isForegroundServiceRunning.collectAsState()
    val isFloatingOverlayActive by viewModel.isFloatingOverlayActive.collectAsState()
    val isFloatingButtonEnabled by viewModel.isFloatingButtonEnabled.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val screenText by viewModel.currentScreenText.collectAsState()
    val screenPackage by viewModel.currentScreenPackage.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val queuedActions by viewModel.queuedActions.collectAsState()
    val pendingQueueCount = queuedActions.count { it.status == "QUEUED" }


    // Permission checks
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    var isBatteryIgnored by remember {
        mutableStateOf(
            runCatching {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            }.getOrDefault(false)
        )
    }

    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Title Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 24.dp, backgroundColor = DeepSpaceSurface)
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ASSISTANT STATUS & SYSTEM DIAGNOSTICS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Monitor background service, wake word, accessibility, and permissions",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Status Diagnostics Grid
        StatusDiagnosticCard(
            title = "Wake Word ('Hey Jarvis')",
            subtitle = if (isWakeWordEnabled) "Listening for wake phrase automatically" else "Wake word detection disabled",
            icon = Icons.Default.RecordVoiceOver,
            isActive = isWakeWordEnabled,
            actionLabel = if (isWakeWordEnabled) "ACTIVE" else "ENABLE",
            onAction = { viewModel.updateWakeWord(!isWakeWordEnabled) }
        )

        StatusDiagnosticCard(
            title = "Offline Smart Action Queue",
            subtitle = if (isOnline) "Network online. Pending queued actions: $pendingQueueCount" else "Offline mode active. Pending queued actions: $pendingQueueCount",
            icon = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
            isActive = pendingQueueCount == 0,
            actionLabel = if (pendingQueueCount > 0) "RUN QUEUE ($pendingQueueCount)" else if (isOnline) "READY" else "OFFLINE ACTIVE",
            onAction = { viewModel.processQueuedActionsAutomatically() }
        )


        StatusDiagnosticCard(
            title = "Background Assistant Service",
            subtitle = if (isBgServiceRunning) "Service running in foreground notification" else "Background service stopped",
            icon = Icons.Default.FlipToFront,
            isActive = isBgServiceRunning,
            actionLabel = if (isBgServiceEnabled) "RUNNING" else "START SERVICE",
            onAction = { viewModel.toggleBackgroundService(!isBgServiceEnabled) }
        )

        StatusDiagnosticCard(
            title = "Screen Awareness (Accessibility)",
            subtitle = if (isAccessibilityActive) "Active app context: '$screenPackage'" else "Grant Accessibility Permission to enable screen reading",
            icon = Icons.Default.Visibility,
            isActive = isAccessibilityActive,
            actionLabel = if (isAccessibilityActive) "CONNECTED" else "ENABLE IN SETTINGS",
            onAction = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )

        StatusDiagnosticCard(
            title = "Microphone Permission",
            subtitle = if (hasMicPermission) "Audio recording permission granted" else "Microphone access required for voice recognition",
            icon = Icons.Default.Mic,
            isActive = hasMicPermission,
            actionLabel = if (hasMicPermission) "GRANTED" else "GRANT MIC",
            onAction = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )

        StatusDiagnosticCard(
            title = "Battery Optimization Status",
            subtitle = if (isBatteryIgnored) "Unrestricted - Optimized for background wake word" else "Optimized by Android - May sleep during long idle",
            icon = Icons.Default.BatteryChargingFull,
            isActive = isBatteryIgnored,
            actionLabel = if (isBatteryIgnored) "UNRESTRICTED" else "OPTIMIZE BATTERY",
            onAction = {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Open Battery Settings to disable restrictions", Toast.LENGTH_SHORT).show()
                }
            }
        )

        StatusDiagnosticCard(
            title = "Floating Assistant Overlay Widget",
            subtitle = if (isFloatingOverlayActive) "Overlay button active on screen" else "Enable floating quick access button over other apps",
            icon = Icons.Default.Layers,
            isActive = isFloatingOverlayActive,
            actionLabel = if (isFloatingButtonEnabled) "ACTIVE" else "ENABLE OVERLAY",
            onAction = {
                if (!Settings.canDrawOverlays(context)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } else {
                    viewModel.toggleFloatingOverlay(!isFloatingButtonEnabled)
                }
            }
        )

        // Live Screen Awareness Inspector Card
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
                        Icon(Icons.Default.FindInPage, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LIVE SCREEN CONTENT READOUT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    if (isAccessibilityActive) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldGreen.copy(alpha = 0.25f)
                        ) {
                            Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (screenText.isNotBlank()) screenText.take(600) + if (screenText.length > 600) "..." else ""
                    else "No screen text captured yet. Open another app or switch screens after granting Accessibility permission.",
                    fontSize = 11.sp,
                    color = if (screenText.isNotBlank()) TextPrimary else TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GlassSurfaceDark, shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusDiagnosticCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 20.dp, backgroundColor = DeepSpaceSurface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isActive) NeonCyan.copy(alpha = 0.2f) else GlassSurfaceDark)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) NeonCyan else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isActive) EmeraldGreen.copy(alpha = 0.25f) else ElectricBlue.copy(alpha = 0.25f),
                modifier = Modifier.clickable { onAction() }
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) EmeraldGreen else NeonCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
