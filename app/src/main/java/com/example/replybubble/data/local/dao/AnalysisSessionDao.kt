package com.example.replybubble.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.replybubble.data.local.entity.AnalysisSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisSessionDao {
    @Query("SELECT * FROM analysis_sessions ORDER BY createdAt DESC")
    fun observeRecent(): Flow<List<AnalysisSessionEntity>>

    @Query("SELECT * FROM analysis_sessions ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(): Flow<AnalysisSessionEntity?>

    @Query("SELECT * FROM analysis_sessions WHERE id = :sessionId LIMIT 1")
    fun observeById(sessionId: Long): Flow<AnalysisSessionEntity?>

    @Query("SELECT * FROM analysis_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: Long): AnalysisSessionEntity?

    @Insert
    suspend fun insert(entity: AnalysisSessionEntity): Long

    @Query("DELETE FROM analysis_sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: Long)

    @Query("DELETE FROM analysis_sessions")
    suspend fun clearAll()
}
