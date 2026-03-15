package com.example.replybubble.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_profiles")
data class ContactProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationshipType: String,
    val toneStyle: String,
    val constraints: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
)
