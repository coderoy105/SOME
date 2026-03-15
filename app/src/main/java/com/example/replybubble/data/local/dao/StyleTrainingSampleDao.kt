package com.example.replybubble.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.replybubble.data.local.entity.StyleTrainingSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StyleTrainingSampleDao {
    @Query("SELECT * FROM style_training_samples ORDER BY promptId ASC, updatedAt DESC")
    fun observeAll(): Flow<List<StyleTrainingSampleEntity>>

    @Query("SELECT * FROM style_training_samples ORDER BY promptId ASC, updatedAt DESC")
    suspend fun getAll(): List<StyleTrainingSampleEntity>

    @Query("SELECT * FROM style_training_samples WHERE promptId = :promptId LIMIT 1")
    suspend fun getByPromptId(promptId: Int): StyleTrainingSampleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StyleTrainingSampleEntity): Long

    @Query("DELETE FROM style_training_samples WHERE id = :sampleId")
    suspend fun deleteById(sampleId: Long)

    @Query("DELETE FROM style_training_samples")
    suspend fun clearAll()
}
