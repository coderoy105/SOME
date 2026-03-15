package com.example.replybubble.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.replybubble.data.local.entity.ReplySuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReplySuggestionDao {
    @Query("SELECT * FROM reply_suggestions WHERE sessionId = :sessionId ORDER BY id ASC")
    fun observeBySessionId(sessionId: Long): Flow<List<ReplySuggestionEntity>>

    @Query("SELECT * FROM reply_suggestions WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getBySessionId(sessionId: Long): List<ReplySuggestionEntity>

    @Query("SELECT * FROM reply_suggestions WHERE id = :replyId LIMIT 1")
    suspend fun getById(replyId: Long): ReplySuggestionEntity?

    @Insert
    suspend fun insertAll(entities: List<ReplySuggestionEntity>)

    @Query("DELETE FROM reply_suggestions WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)

    @Query("UPDATE reply_suggestions SET copiedCount = copiedCount + 1 WHERE id = :replyId")
    suspend fun incrementCopyCount(replyId: Long)

    @Query(
        "UPDATE reply_suggestions SET content = :content, copiedCount = :copiedCount, createdAt = :createdAt WHERE id = :replyId",
    )
    suspend fun updateReply(
        replyId: Long,
        content: String,
        copiedCount: Int,
        createdAt: Long,
    )

    @Query("DELETE FROM reply_suggestions")
    suspend fun clearAll()
}
