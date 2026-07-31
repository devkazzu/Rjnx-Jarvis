package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "USER" or "JARVIS"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = false
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckName: String,
    val question: String,
    val answer: String,
    val isLearned: Boolean = false
)

@Entity(tableName = "todos")
data class TodoItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val category: String = "General",
    val priority: String = "Medium", // High, Medium, Low
    val dueDateMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val streakCount: Int = 0,
    val targetDaysPerWeek: Int = 7,
    val lastCompletedDate: String = "", // YYYY-MM-DD
    val iconName: String = "Check"
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val isIncome: Boolean = false,
    val dateMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timeMillis: Long,
    val isCompleted: Boolean = false
)

@Entity(tableName = "exam_countdowns")
data class ExamCountdownEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examTitle: String,
    val subject: String,
    val examDateMillis: Long,
    val targetGrade: String = "A+"
)

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalTitle: String,
    val targetCount: Int = 1,
    val currentCount: Int = 0,
    val isCompleted: Boolean = false
)

@Entity(tableName = "queued_actions")
data class QueuedActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val actionType: String,
    val target: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "QUEUED" // "QUEUED", "EXECUTED", "FAILED"
)

