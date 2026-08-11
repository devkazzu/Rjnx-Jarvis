package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        QueuedActionEntity::class,
        MemoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS memories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, key TEXT NOT NULL, value TEXT NOT NULL, category TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            }
        }

        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getDatabase(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
