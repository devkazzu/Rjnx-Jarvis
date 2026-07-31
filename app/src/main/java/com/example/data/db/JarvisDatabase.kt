package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatMessageEntity::class,
        NoteEntity::class,
        FlashcardEntity::class,
        TodoItemEntity::class,
        HabitEntity::class,
        ExpenseEntity::class,
        ReminderEntity::class,
        ExamCountdownEntity::class,
        DailyGoalEntity::class,
        QueuedActionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getDatabase(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
