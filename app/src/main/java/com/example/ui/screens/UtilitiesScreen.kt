package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.JarvisViewModel

@Composable
fun UtilitiesScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    var activeTool by remember { mutableStateOf("QR Tools") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tools = listOf("QR Tools", "Flashlight HUD", "File Manager")
            tools.forEach { tool ->
                val isSelected = tool == activeTool
                Box(
                    modifier = Modifier
                        .glassCard(
                            cornerRadius = 16.dp,
                            backgroundColor = if (isSelected) QuantumPurple.copy(alpha = 0.3f) else GlassSurfaceDark
                        )
                        .clickable { activeTool = tool }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tool,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) GlowingMagenta else TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTool) {
                "QR Tools" -> QrToolSection()
                "Flashlight HUD" -> FlashlightSection(viewModel)
                "File Manager" -> FileManagerSection()
            }
        }
    }
}

@Composable
fun QrToolSection() {
    var qrText by remember { mutableStateOf("https://ai.studio") }
    var scannedResult by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("QR CODE GENERATOR", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = qrText,
                    onValueChange = { qrText = it },
                    label = { Text("Text or URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // QR Visual Grid representation
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .glassCard(cornerRadius = 12.dp, backgroundColor = Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val side = size.width / 8f
                        val seed = qrText.hashCode()
                        for (r in 0 until 8) {
                            for (c in 0 until 8) {
                                val isBlack = ((seed + r * 7 + c * 13) % 2 == 0) || (r in 0..2 && c in 0..2) || (r in 0..2 && c in 5..7)
                                if (isBlack) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = androidx.compose.ui.geometry.Offset(c * side, r * side),
                                        size = androidx.compose.ui.geometry.Size(side, side)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DOCUMENT & QR SCANNER", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { scannedResult = "Scanned: RJNX-JARVIS-AUTH-CODE-9921" },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = VoidBackground)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START CAMERA SCANNER", color = VoidBackground, fontWeight = FontWeight.Bold)
                }

                if (scannedResult.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(scannedResult, fontSize = 12.sp, color = NeonCyan)
                }
            }
        }
    }
}

@Composable
fun FlashlightSection(viewModel: JarvisViewModel) {
    val isTorchOn by viewModel.isFlashlightOn.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .glassCard(cornerRadius = 90.dp, backgroundColor = if (isTorchOn) CyberAmber.copy(alpha = 0.35f) else GlassSurfaceDark)
                .clickable { viewModel.toggleFlashlight() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FlashlightOn,
                    contentDescription = "Flashlight",
                    tint = if (isTorchOn) CyberAmber else TextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isTorchOn) "FLASHLIGHT ON" else "FLASHLIGHT OFF",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTorchOn) CyberAmber else TextSecondary
                )
            }
        }
    }
}

@Composable
fun FileManagerSection() {
    val mockFiles = listOf(
        Pair("Jarvis_Neural_Notes.pdf", "2.4 MB"),
        Pair("Physics_Final_Exam.pdf", "5.1 MB"),
        Pair("Study_Schedule.txt", "12 KB"),
        Pair("Project_Architecture.png", "1.8 MB"),
        Pair("Expense_Report_2026.csv", "45 KB")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text("SYSTEM FILE EXPLORER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(bottom = 12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(mockFiles) { (name, size) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 16.dp)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(size, fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
