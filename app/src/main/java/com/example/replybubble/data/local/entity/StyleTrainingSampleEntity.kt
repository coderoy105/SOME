package com.example.replybubble.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "style_training_samples")
data class StyleTrainingSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val promptId: Int,
    val prompt: String,
    val answer: String,
    val createdAt: Long,
    val updatedAt: Long,
)
