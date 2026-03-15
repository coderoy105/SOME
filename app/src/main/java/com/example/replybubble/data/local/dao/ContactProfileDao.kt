package com.example.replybubble.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.replybubble.data.local.entity.ContactProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactProfileDao {
    @Query("SELECT * FROM contact_profiles ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ContactProfileEntity>>

    @Query("SELECT * FROM contact_profiles WHERE id = :profileId LIMIT 1")
    fun observeById(profileId: Long): Flow<ContactProfileEntity?>

    @Query("SELECT * FROM contact_profiles WHERE id = :profileId LIMIT 1")
    suspend fun getById(profileId: Long): ContactProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ContactProfileEntity): Long

    @Query("DELETE FROM contact_profiles WHERE id = :profileId")
    suspend fun deleteById(profileId: Long)

    @Query("DELETE FROM contact_profiles")
    suspend fun clearAll()
}
