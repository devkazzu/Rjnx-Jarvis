package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.FlashcardEntity
import com.example.ui.theme.*
import com.example.viewmodel.JarvisViewModel

@Composable
fun StudyAssistantScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf("Doubt Solver") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Study Tabs Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("Doubt Solver", "Summarizer", "Notes", "Flashcards", "Pomodoro", "Exams")
            items(tabs) { tab ->
                val isSelected = tab == activeTab
                Box(
                    modifier = Modifier
                        .glassCard(
                            cornerRadius = 16.dp,
                            backgroundColor = if (isSelected) QuantumPurple.copy(alpha = 0.3f) else GlassSurfaceDark
                        )
                        .clickable { activeTab = tab }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab,
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
            when (activeTab) {
                "Doubt Solver" -> DoubtSolverTab(viewModel)
                "Summarizer" -> SummarizerTab(viewModel)
                "Notes" -> NotesTab(viewModel)
                "Flashcards" -> FlashcardsTab(viewModel)
                "Pomodoro" -> PomodoroTab(viewModel)
                "Exams" -> ExamCountdownTab(viewModel)
            }
        }
    }
}

@Composable
fun DoubtSolverTab(viewModel: JarvisViewModel) {
    var subject by remember { mutableStateOf("Mathematics") }
    var doubtText by remember { mutableStateOf("") }
    val doubtResult by viewModel.doubtResult.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Text("AI STEP-BY-STEP DOUBT SOLVER", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (e.g. Physics, Math, CS)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = doubtText,
                    onValueChange = { doubtText = it },
                    placeholder = { Text("Paste your question or doubt here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.solveDoubt(subject, doubtText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = QuantumPurple),
                    enabled = !isGenerating && doubtText.isNotBlank()
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("SOLVE WITH JARVIS AI", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        if (doubtResult.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
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
                        Text("SOLUTION EXPLANATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        IconButton(onClick = { viewModel.copyToClipboard(doubtResult) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = doubtResult, fontSize = 13.sp, color = TextPrimary, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
fun SummarizerTab(viewModel: JarvisViewModel) {
    var docName by remember { mutableStateOf("Physics Chapter 4") }
    var contentText by remember { mutableStateOf("") }
    val summaryResult by viewModel.summaryResult.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(16.dp)
        ) {
            Column {
                Text("DOCUMENT & PDF SUMMARIZER", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = docName,
                    onValueChange = { docName = it },
                    label = { Text("Document Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    placeholder = { Text("Paste document text or notes to summarize...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.summarizeDocument(docName, contentText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    enabled = !isGenerating && contentText.isNotBlank()
                ) {
                    Text("GENERATE AI SUMMARY", fontWeight = FontWeight.Bold, color = VoidBackground)
                }
            }
        }

        if (summaryResult.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 20.dp)
                    .padding(16.dp)
            ) {
                Column {
                    Text("EXECUTIVE SUMMARY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GlowingMagenta)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = summaryResult, fontSize = 13.sp, color = TextPrimary, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
fun NotesTab(viewModel: JarvisViewModel) {
    val notes by viewModel.notes.collectAsState()
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 18.dp)
                .padding(14.dp)
        ) {
            Column {
                Text("ADD STUDY NOTE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, placeholder = { Text("Note Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = newContent, onValueChange = { newContent = it }, placeholder = { Text("Content...") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.addNote(newTitle, newContent)
                        newTitle = ""
                        newContent = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassSurfaceLight)
                ) {
                    Text("SAVE NOTE", color = NeonCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notes, key = { it.id }) { note ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 16.dp)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(note.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            IconButton(onClick = { viewModel.deleteNote(note) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(note.content, fontSize = 12.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardsTab(viewModel: JarvisViewModel) {
    val cards by viewModel.flashcards.collectAsState()
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 18.dp)
                .padding(14.dp)
        ) {
            Column {
                Text("CREATE FLASHCARD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = question, onValueChange = { question = it }, placeholder = { Text("Question / Term") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = answer, onValueChange = { answer = it }, placeholder = { Text("Answer / Definition") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.addFlashcard("General", question, answer)
                        question = ""
                        answer = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = QuantumPurple)
                ) {
                    Text("ADD FLASHCARD", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cards, key = { it.id }) { card ->
                FlashcardItem(card = card, onToggleLearned = { viewModel.toggleFlashcardLearned(card.id, !card.isLearned) }, onDelete = { viewModel.deleteFlashcard(card) })
            }
        }
    }
}

@Composable
fun FlashcardItem(card: FlashcardEntity, onToggleLearned: () -> Unit, onDelete: () -> Unit) {
    var flipped by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 18.dp, backgroundColor = if (flipped) GlassSurfaceLight else GlassSurfaceDark)
            .clickable { flipped = !flipped }
            .padding(16.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (flipped) "ANSWER (Tap to flip back)" else "QUESTION (Tap to flip)", fontSize = 10.sp, color = TextSecondary)
                IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (flipped) card.answer else card.question,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (flipped) GlowingMagenta else NeonCyan
            )
        }
    }
}

@Composable
fun PomodoroTab(viewModel: JarvisViewModel) {
    val timeLeft by viewModel.pomodoroTimeLeft.collectAsState()
    val isRunning by viewModel.isPomodoroRunning.collectAsState()
    val mode by viewModel.pomodoroMode.collectAsState()

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val formatTime = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .glassCard(cornerRadius = 120.dp, backgroundColor = GlassSurfaceDark)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(mode.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Spacer(modifier = Modifier.height(8.dp))
                Text(formatTime, fontSize = 52.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { if (isRunning) viewModel.pausePomodoro() else viewModel.startPomodoro() },
                colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) GlowingMagenta else NeonCyan),
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = VoidBackground)
            }

            Button(
                onClick = { viewModel.resetPomodoro() },
                colors = ButtonDefaults.buttonColors(containerColor = GlassSurfaceLight),
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary)
            }
        }
    }
}

@Composable
fun ExamCountdownTab(viewModel: JarvisViewModel) {
    val exams by viewModel.examCountdowns.collectAsState()
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 18.dp)
                .padding(14.dp)
        ) {
            Column {
                Text("ADD UPCOMING EXAM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("Exam Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = subject, onValueChange = { subject = it }, placeholder = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.addExamCountdown(title, subject, System.currentTimeMillis() + 10 * 86400000L)
                        title = ""
                        subject = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber)
                ) {
                    Text("ADD EXAM COUNTDOWN", color = VoidBackground, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(exams, key = { it.id }) { exam ->
                val daysLeft = ((exam.examDateMillis - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
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
                        Column {
                            Text(exam.examTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(exam.subject, fontSize = 12.sp, color = TextSecondary)
                        }
                        Text("$daysLeft DAYS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberAmber)
                    }
                }
            }
        }
    }
}
