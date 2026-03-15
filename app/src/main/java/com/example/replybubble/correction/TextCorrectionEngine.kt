package com.example.replybubble.correction

interface TextCorrectionEngine {
    suspend fun correct(text: String): String
}
