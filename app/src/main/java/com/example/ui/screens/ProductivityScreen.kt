package com.example.ui.screens

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
import com.example.data.db.TodoItemEntity
import com.example.ui.theme.*
import com.example.viewmodel.JarvisViewModel

@Composable
fun ProductivityScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf("To-Dos") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("To-Dos", "Habits", "Expenses", "Daily Goals")
            items(tabs) { tab ->
                val isSelected = tab == activeSubTab
                Box(
                    modifier = Modifier
                        .glassCard(
                            cornerRadius = 16.dp,
                            backgroundColor = if (isSelected) EmeraldGreen.copy(alpha = 0.25f) else GlassSurfaceDark
                        )
                        .clickable { activeSubTab = tab }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tab,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) EmeraldGreen else TextPrimary
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
            when (activeSubTab) {
                "To-Dos" -> TodoSection(viewModel)
                "Habits" -> HabitSection(viewModel)
                "Expenses" -> ExpenseSection(viewModel)
                "Daily Goals" -> DailyGoalSection(viewModel)
            }
        }
    }
}

@Composable
fun TodoSection(viewModel: JarvisViewModel) {
    val todos by viewModel.todos.collectAsState()
    var title by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("New task title...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.addTodo(title)
                        title = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreen)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = VoidBackground)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(todos, key = { it.id }) { todo ->
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = todo.isCompleted,
                                onCheckedChange = { checked -> viewModel.toggleTodo(todo.id, checked) },
                                colors = CheckboxDefaults.colors(checkedColor = EmeraldGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = todo.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (todo.isCompleted) TextSecondary else TextPrimary
                            )
                        }

                        IconButton(onClick = { viewModel.deleteTodo(todo) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitSection(viewModel: JarvisViewModel) {
    val habits by viewModel.habits.collectAsState()
    var title by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("New Habit title...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.addHabit(title)
                        title = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NeonCyan)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = VoidBackground)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(habits, key = { it.id }) { habit ->
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
                            Text(habit.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Streak: ${habit.streakCount} days", fontSize = 12.sp, color = NeonCyan)
                        }

                        Button(
                            onClick = { viewModel.incrementHabitStreak(habit) },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassSurfaceLight)
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = CyberAmber)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+1 STREAK", color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseSection(viewModel: JarvisViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }

    val totalBalance = expenses.sumOf { if (it.isIncome) it.amount else -it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Balance Overview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp)
                .padding(20.dp)
        ) {
            Column {
                Text("NET EXPENSE BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Text(
                    text = String.format("$%.2f", totalBalance),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalBalance >= 0) EmeraldGreen else GlowingMagenta
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 18.dp)
                .padding(14.dp)
        ) {
            Column {
                Text("ADD TRANSACTION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it }, placeholder = { Text("Amount ($)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !isIncome, onClick = { isIncome = false })
                        Text("Expense", fontSize = 12.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        RadioButton(selected = isIncome, onClick = { isIncome = true })
                        Text("Income", fontSize = 12.sp, color = TextPrimary)
                    }

                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amt > 0) {
                                viewModel.addExpense(title, amt, "General", isIncome)
                                title = ""
                                amount = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = QuantumPurple)
                    ) {
                        Text("ADD", color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        expenses.forEach { exp ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .glassCard(cornerRadius = 14.dp)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(exp.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        text = (if (exp.isIncome) "+" else "-") + String.format("$%.2f", exp.amount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (exp.isIncome) EmeraldGreen else GlowingMagenta
                    )
                }
            }
        }
    }
}

@Composable
fun DailyGoalSection(viewModel: JarvisViewModel) {
    val goals by viewModel.dailyGoals.collectAsState()
    var title by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("New Daily Goal...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.addDailyGoal(title, targetCount = 3)
                        title = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CyberAmber)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = VoidBackground)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(goals, key = { it.id }) { goal ->
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
                            Text(goal.goalTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Progress: ${goal.currentCount} / ${goal.targetCount}", fontSize = 12.sp, color = CyberAmber)
                        }

                        Button(
                            onClick = { viewModel.incrementDailyGoalProgress(goal) },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassSurfaceLight)
                        ) {
                            Text("+1 DONE", color = NeonCyan)
                        }
                    }
                }
            }
        }
    }
}
