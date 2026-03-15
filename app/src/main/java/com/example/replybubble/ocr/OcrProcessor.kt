package com.example.replybubble.ocr

import android.graphics.Bitmap

data class OcrResult(
    val rawText: String,
    val hasText: Boolean,
)

interface OcrProcessor {
    suspend fun recognize(bitmap: Bitmap): OcrResult
}
