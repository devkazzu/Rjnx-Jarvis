package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.utilities.SystemUtils
import com.example.viewmodel.JarvisViewModel
import java.util.Calendar

@Composable
fun SmartAssistantScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf("Shortcuts") }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Sub Navigation Tabs
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("Shortcuts", "Offline Queue", "Alarms & Reminders", "Weather & Web", "Calculator", "Unit Converter")
            items(tabs) { tab ->
                val isSelected = tab == selectedSection
                Box(
                    modifier = Modifier
                        .glassCard(
                            cornerRadius = 16.dp,
                            backgroundColor = if (isSelected) NeonCyan.copy(alpha = 0.25f) else GlassSurfaceDark
                        )
                        .clickable { selectedSection = tab }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) NeonCyan else TextPrimary
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
            when (selectedSection) {
                "Shortcuts" -> AppShortcutsSection(viewModel)
                "Offline Queue" -> OfflineQueueSection(viewModel)
                "Alarms & Reminders" -> AlarmsAndRemindersSection(viewModel)
                "Weather & Web" -> WeatherAndWebSection(viewModel)
                "Calculator" -> CalculatorSection(viewModel)
                "Unit Converter" -> UnitConverterSection()
            }
        }

    }
}

@Composable
fun AppShortcutsSection(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val installedApps by viewModel.installedApps.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "INSTALLED APPS LAUNCHER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(installedApps.take(15)) { app ->
                Box(
                    modifier = Modifier
                        .glassCard(cornerRadius = 16.dp)
                        .clickable { SystemUtils.launchApp(context, app.packageName) }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Apps, contentDescription = app.name, tint = NeonCyan)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = app.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmsAndRemindersSection(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    var alarmHour by remember { mutableStateOf(7) }
    var alarmMinute by remember { mutableStateOf(0) }
    var alarmMsg by remember { mutableStateOf("Jarvis Wake Up") }

    var timerMinutes by remember { mutableStateOf("10") }
    var reminderTitle by remember { mutableStateOf("") }

    val reminders by viewModel.reminders.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Set Alarm Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Alarm, contentDescription = null, tint = CyberAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SET QUICK ALARM", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            TimePickerDialog(context, { _, h, m ->
                                alarmHour = h
                                alarmMinute = m
                            }, alarmHour, alarmMinute, true).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassSurfaceLight)
                    ) {
                        Text(String.format("%02d:%02d", alarmHour, alarmMinute), color = NeonCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = alarmMsg,
                        onValueChange = { alarmMsg = it },
                        label = { Text("Alarm Label", fontSize = 10.sp) },
                        modifier = Modifier.width(160.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { SystemUtils.setAlarm(context, alarmHour, alarmMinute, alarmMsg) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber)
                ) {
                    Text("LAUNCH SYSTEM ALARM", color = VoidBackground, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Set Timer Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SET SYSTEM TIMER", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = timerMinutes,
                        onValueChange = { timerMinutes = it },
                        label = { Text("Minutes") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val mins = timerMinutes.toIntOrNull() ?: 5
                            SystemUtils.setTimer(context, mins * 60, "Jarvis Timer")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("START TIMER", color = VoidBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Create Reminder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Text("CREATE REMINDER", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = reminderTitle,
                        onValueChange = { reminderTitle = it },
                        placeholder = { Text("Reminder description...") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (reminderTitle.isNotBlank()) {
                                viewModel.addReminder(reminderTitle, System.currentTimeMillis() + 3600000)
                                reminderTitle = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(QuantumPurple)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                reminders.forEach { reminder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(reminder.title, fontSize = 13.sp, color = TextPrimary)
                        IconButton(onClick = { viewModel.deleteReminder(reminder) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherAndWebSection(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Futuristic Weather Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 24.dp)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CURRENT LOCATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        Text("San Francisco, CA", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Icon(Icons.Default.WbSunny, contentDescription = "Weather", tint = CyberAmber, modifier = Modifier.size(44.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text("24°C", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Clear Skies", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeonCyan)
                        Text("Humidity: 48% | Wind: 12 km/h", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Web Search Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Text("PERFORM GOOGLE SEARCH", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search query...") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                SystemUtils.performGoogleSearch(context, searchQuery)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Open Website Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Text("OPEN WEBSITE URL", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it },
                        placeholder = { Text("https://example.com") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (websiteUrl.isNotBlank()) {
                                SystemUtils.openWebsite(context, websiteUrl)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GlowingMagenta)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = "Open", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorSection(viewModel: JarvisViewModel) {
    val display by viewModel.calcDisplay.collectAsState()

    val buttons = listOf(
        listOf("C", "(", ")", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "DEL", "=")
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = display,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Keypad Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            buttons.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { btn ->
                        val isOp = btn in listOf("C", "(", ")", "÷", "×", "-", "+", "=", "DEL")
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .glassCard(
                                    cornerRadius = 16.dp,
                                    backgroundColor = if (btn == "=") NeonCyan else if (isOp) GlassSurfaceLight else GlassSurfaceDark
                                )
                                .clickable { viewModel.onCalcButtonPress(btn) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = btn,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (btn == "=") VoidBackground else if (isOp) GlowingMagenta else TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnitConverterSection() {
    var category by remember { mutableStateOf("Length") }
    var inputValue by remember { mutableStateOf("100") }
    var fromUnit by remember { mutableStateOf("Meter") }
    var toUnit by remember { mutableStateOf("Kilometer") }

    val categories = listOf("Length", "Mass", "Temperature", "Speed")
    val unitsMap = mapOf(
        "Length" to listOf("Meter", "Kilometer", "Centimeter", "Mile", "Foot", "Inch"),
        "Mass" to listOf("Kilogram", "Gram", "Pound", "Ounce"),
        "Temperature" to listOf("Celsius", "Fahrenheit", "Kelvin"),
        "Speed" to listOf("km/h", "mph", "m/s")
    )

    val currentUnits = unitsMap[category] ?: emptyList()
    val result = SystemUtils.convertUnit(inputValue.toDoubleOrNull() ?: 0.0, category, fromUnit, toUnit)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Category Selector
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                val isSelected = cat == category
                Box(
                    modifier = Modifier
                        .glassCard(
                            cornerRadius = 14.dp,
                            backgroundColor = if (isSelected) QuantumPurple.copy(alpha = 0.3f) else GlassSurfaceDark
                        )
                        .clickable {
                            category = cat
                            fromUnit = currentUnits.firstOrNull() ?: ""
                            toUnit = currentUnits.getOrNull(1) ?: fromUnit
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(cat, fontSize = 12.sp, color = if (isSelected) GlowingMagenta else TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Conversion Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(20.dp)
        ) {
            Column {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text("Value to convert") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("FROM", fontSize = 10.sp, color = TextSecondary)
                        currentUnits.take(4).forEach { u ->
                            Text(
                                text = u,
                                fontSize = 12.sp,
                                fontWeight = if (u == fromUnit) FontWeight.Bold else FontWeight.Normal,
                                color = if (u == fromUnit) NeonCyan else TextPrimary,
                                modifier = Modifier
                                    .clickable { fromUnit = u }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("TO", fontSize = 10.sp, color = TextSecondary)
                        currentUnits.take(4).forEach { u ->
                            Text(
                                text = u,
                                fontSize = 12.sp,
                                fontWeight = if (u == toUnit) FontWeight.Bold else FontWeight.Normal,
                                color = if (u == toUnit) GlowingMagenta else TextPrimary,
                                modifier = Modifier
                                    .clickable { toUnit = u }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 14.dp, backgroundColor = GlassSurfaceLight)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("RESULT: $result $toUnit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                }
            }
        }
    }
}

@Composable
fun OfflineQueueSection(viewModel: JarvisViewModel) {
    val isOnline by viewModel.isOnline.collectAsState()
    val queuedActions by viewModel.queuedActions.collectAsState()
    var manualCommandInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Network Status Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(
                    cornerRadius = 20.dp,
                    backgroundColor = if (isOnline) DeepSpaceSurface else Color(0xFF2A1518)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) EmeraldGreen.copy(alpha = 0.2f) else BrightRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isOnline) EmeraldGreen else BrightRed
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isOnline) "NETWORK CONNECTED" else "OFFLINE MODE ACTIVE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) EmeraldGreen else BrightRed
                        )
                        Text(
                            text = if (isOnline) "Pending Smart Actions will execute automatically" else "Commands captured locally in queue",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeonCyan.copy(alpha = 0.2f),
                    modifier = Modifier.clickable { viewModel.processQueuedActionsAutomatically() }
                ) {
                    Text(
                        text = "FLUSH QUEUE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Add Manual Queue Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "QUEUE SMART ACTION MANUALLY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = manualCommandInput,
                    onValueChange = { manualCommandInput = it },
                    placeholder = { Text("e.g. Set alarm 7:30 AM, Call 12345, Open YouTube", fontSize = 11.sp, color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            if (manualCommandInput.isNotBlank()) {
                                viewModel.queueSmartActionManually(manualCommandInput)
                                manualCommandInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = VoidBackground, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add to Queue", fontSize = 12.sp, color = VoidBackground, fontWeight = FontWeight.Bold)
                    }


                    TextButton(
                        onClick = { viewModel.clearExecutedQueuedActions() }
                    ) {
                        Text("Clear Completed", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        // List of Queued Actions
        Text(
            text = "QUEUED & EXECUTED ACTIONS (${queuedActions.size})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )

        if (queuedActions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 16.dp)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No actions in local queue", fontSize = 12.sp, color = TextSecondary)
                    Text("Speak commands while offline to capture them here.", fontSize = 10.sp, color = TextSecondary)
                }
            }
        } else {
            queuedActions.forEach { action ->
                val isPending = action.status == "QUEUED"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 16.dp, backgroundColor = DeepSpaceSurface)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isPending) CyberAmber.copy(alpha = 0.25f) else EmeraldGreen.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = action.status,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPending) CyberAmber else EmeraldGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = action.actionType,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = action.command,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            if (action.target.isNotBlank()) {
                                Text(
                                    text = "Target: ${action.target}",
                                    fontSize = 11.sp,
                                    color = ElectricBlue
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.deleteQueuedAction(action.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

