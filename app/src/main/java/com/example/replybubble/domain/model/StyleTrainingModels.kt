package com.example.replybubble.domain.model

data class StyleTrainingSample(
    val id: Long,
    val promptId: Int,
    val prompt: String,
    val answer: String,
    val createdAt: Long,
    val updatedAt: Long,
)
