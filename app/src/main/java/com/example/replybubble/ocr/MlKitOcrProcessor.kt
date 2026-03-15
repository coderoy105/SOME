package com.example.replybubble.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class MlKitOcrProcessor @Inject constructor() : OcrProcessor {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(bitmap: Bitmap): OcrResult {
        return runCatching {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val text = recognizer.process(inputImage).await().text.orEmpty().trim()
            OcrResult(rawText = text, hasText = text.isNotBlank())
        }.getOrElse {
            OcrResult(rawText = "", hasText = false)
        }
    }
}
