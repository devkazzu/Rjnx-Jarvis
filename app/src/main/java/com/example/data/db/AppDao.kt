package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Chat
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteChatMessage(id: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllChatMessages()

    // Notes
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    // Flashcards
    @Query("SELECT * FROM flashcards")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long

    @Query("UPDATE flashcards SET isLearned = :isLearned WHERE id = :id")
    suspend fun updateFlashcardLearned(id: Long, isLearned: Boolean)

    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)

    // To-Do
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, dueDateMillis ASC")
    fun getAllTodos(): Flow<List<TodoItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItemEntity): Long

    @Query("UPDATE todos SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTodoStatus(id: Long, isCompleted: Boolean)

    @Delete
    suspend fun deleteTodo(todo: TodoItemEntity)

    // Habits
    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Query("UPDATE habits SET streakCount = :streak, lastCompletedDate = :date WHERE id = :id")
    suspend fun updateHabitStreak(id: Long, streak: Int, date: String)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    // Expenses
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // Reminders
    @Query("SELECT * FROM reminders ORDER BY timeMillis ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Query("UPDATE reminders SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateReminderStatus(id: Long, isCompleted: Boolean)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    // Exams
    @Query("SELECT * FROM exam_countdowns ORDER BY examDateMillis ASC")
    fun getAllExamCountdowns(): Flow<List<ExamCountdownEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamCountdown(exam: ExamCountdownEntity): Long

    @Delete
    suspend fun deleteExamCountdown(exam: ExamCountdownEntity)

    // Daily Goals
    @Query("SELECT * FROM daily_goals")
    fun getAllDailyGoals(): Flow<List<DailyGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyGoal(goal: DailyGoalEntity): Long

    @Query("UPDATE daily_goals SET currentCount = :count, isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateDailyGoalProgress(id: Long, count: Int, isCompleted: Boolean)

    @Delete
    suspend fun deleteDailyGoal(goal: DailyGoalEntity)

    // Long-term Memory
    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE key = :key LIMIT 1")
    suspend fun findMemory(key: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE key LIKE '%' || :query || '%' OR value LIKE '%' || :query || '%' ORDER BY updatedAt DESC LIMIT 12")
    suspend fun searchMemories(query: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM memories")
    suspend fun clearAllMemories()

    // Queued Smart Actions
    @Query("SELECT * FROM queued_actions ORDER BY timestamp DESC")
    fun getAllQueuedActions(): Flow<List<QueuedActionEntity>>

    @Query("SELECT * FROM queued_actions WHERE status = 'QUEUED' ORDER BY timestamp ASC")
    suspend fun getPendingQueuedActions(): List<QueuedActionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueuedAction(action: QueuedActionEntity): Long

    @Query("UPDATE queued_actions SET status = :status WHERE id = :id")
    suspend fun updateQueuedActionStatus(id: Long, status: String)

    @Query("DELETE FROM queued_actions WHERE id = :id")
    suspend fun deleteQueuedAction(id: Long)

    @Query("DELETE FROM queued_actions WHERE status = 'EXECUTED'")
    suspend fun clearExecutedQueuedActions()
}

