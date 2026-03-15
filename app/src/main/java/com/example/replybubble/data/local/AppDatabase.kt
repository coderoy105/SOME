package com.example.replybubble.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.replybubble.data.local.dao.AnalysisSessionDao
import com.example.replybubble.data.local.dao.ContactProfileDao
import com.example.replybubble.data.local.dao.ReplySuggestionDao
import com.example.replybubble.data.local.dao.StyleTrainingSampleDao
import com.example.replybubble.data.local.entity.AnalysisSessionEntity
import com.example.replybubble.data.local.entity.ContactProfileEntity
import com.example.replybubble.data.local.entity.ReplySuggestionEntity
import com.example.replybubble.data.local.entity.StyleTrainingSampleEntity

@Database(
    entities = [
        ContactProfileEntity::class,
        AnalysisSessionEntity::class,
        ReplySuggestionEntity::class,
        StyleTrainingSampleEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactProfileDao(): ContactProfileDao
    abstract fun analysisSessionDao(): AnalysisSessionDao
    abstract fun replySuggestionDao(): ReplySuggestionDao
    abstract fun styleTrainingSampleDao(): StyleTrainingSampleDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `style_training_samples` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `promptId` INTEGER NOT NULL,
                        `prompt` TEXT NOT NULL,
                        `answer` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
