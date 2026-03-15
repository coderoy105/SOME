package com.example.replybubble.recommendation

import android.util.Log
import com.example.replybubble.BuildConfig
import com.example.replybubble.correction.OpenRouterCorrectionPromptBuilder
import com.example.replybubble.domain.model.ReplyCategory
import com.example.replybubble.domain.model.StyleTrainingSample
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class OpenRouterApiClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun generateReplies(request: RecommendationRequest): List<GeneratedReply>? {
        val apiKey = BuildConfig.OPENROUTER_API_KEY.trim()
        if (apiKey.isBlank()) return null

        return withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject().apply {
                    put("model", BuildConfig.OPENROUTER_MODEL.ifBlank { DEFAULT_MODEL })
                    put("temperature", 0.55)
                    put("top_p", 0.9)
                    put("max_completion_tokens", 420)
                    put("response_format", JSONObject().put("type", "json_object"))
                    put("messages", buildMessages(request))
                }

                val requestBuilder = Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

                if (BuildConfig.OPENROUTER_REFERER.isNotBlank()) {
                    requestBuilder.addHeader("HTTP-Referer", BuildConfig.OPENROUTER_REFERER)
                }
                if (BuildConfig.OPENROUTER_TITLE.isNotBlank()) {
                    requestBuilder.addHeader("X-OpenRouter-Title", BuildConfig.OPENROUTER_TITLE)
                }

                okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "OpenRouter request failed: ${response.code}")
                        return@use emptyList()
                    }
                    parseSuggestions(response.body?.string().orEmpty())
                }
            }.onFailure { throwable ->
                Log.e(TAG, "OpenRouter request error", throwable)
            }.getOrNull()
        }
    }

    suspend fun correctText(text: String): String? {
        val apiKey = BuildConfig.OPENROUTER_API_KEY.trim()
        if (apiKey.isBlank()) return null

        return withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject().apply {
                    put("model", BuildConfig.OPENROUTER_MODEL.ifBlank { DEFAULT_MODEL })
                    put("temperature", 0.2)
                    put("top_p", 0.85)
                    put("max_completion_tokens", 180)
                    put("response_format", JSONObject().put("type", "json_object"))
                    put(
                        "messages",
                        JSONArray()
                            .put(
                                JSONObject()
                                    .put("role", "system")
                                    .put("content", OpenRouterCorrectionPromptBuilder.buildSystemPrompt()),
                            )
                            .put(
                                JSONObject()
                                    .put("role", "user")
                                    .put("content", OpenRouterCorrectionPromptBuilder.buildUserPrompt(text)),
                            ),
                    )
                }

                val requestBuilder = Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

                if (BuildConfig.OPENROUTER_REFERER.isNotBlank()) {
                    requestBuilder.addHeader("HTTP-Referer", BuildConfig.OPENROUTER_REFERER)
                }
                if (BuildConfig.OPENROUTER_TITLE.isNotBlank()) {
                    requestBuilder.addHeader("X-OpenRouter-Title", BuildConfig.OPENROUTER_TITLE)
                }

                okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "OpenRouter correction request failed: ${response.code}")
                        return@use null
                    }
                    parseCorrectedText(response.body?.string().orEmpty())
                }
            }.onFailure { throwable ->
                Log.e(TAG, "OpenRouter correction request error", throwable)
            }.getOrNull()
        }
    }

    private fun parseSuggestions(responseBody: String): List<GeneratedReply> {
        val messageContent = extractMessageContent(responseBody)
        if (messageContent.isBlank()) return emptyList()

        val normalizedJson = normalizeJsonBlock(messageContent)
        val root = JSONObject(normalizedJson)
        val suggestions = root.optJSONArray("suggestions")
            ?: root.optJSONArray("replies")
            ?: JSONArray()

        return buildList {
            for (index in 0 until suggestions.length()) {
                val item = suggestions.optJSONObject(index) ?: continue
                val content = item.optString("content").trim()
                if (content.isBlank()) continue
                add(
                    GeneratedReply(
                        category = parseCategory(item.optString("category"), index),
                        content = content,
                    ),
                )
            }
        }
    }

    private fun parseCorrectedText(responseBody: String): String? {
        val messageContent = extractMessageContent(responseBody)
        if (messageContent.isBlank()) return null

        val normalizedJson = normalizeJsonBlock(messageContent)
        val root = JSONObject(normalizedJson)
        return root.optString("correctedText")
            .trim()
            .takeIf { it.isNotBlank() }
    }

    private fun extractMessageContent(responseBody: String): String {
        val root = JSONObject(responseBody)
        val message = root.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?: return ""
        val content = message.opt("content") ?: return ""

        return when (content) {
            is String -> content
            is JSONArray -> buildString {
                for (index in 0 until content.length()) {
                    val part = content.opt(index)
                    when (part) {
                        is JSONObject -> append(part.optString("text"))
                        is String -> append(part)
                    }
                }
            }
            else -> content.toString()
        }
    }

    private fun normalizeJsonBlock(rawContent: String): String {
        val trimmed = rawContent.trim()
        if (!trimmed.startsWith("```")) return trimmed

        return trimmed
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun buildMessages(request: RecommendationRequest): JSONArray {
        return JSONArray()
            .put(
                JSONObject()
                    .put("role", "system")
                    .put("content", OpenRouterPromptBuilder.buildSystemPrompt()),
            )
            .apply {
                appendStyleExamples(this, request.styleSamples)
                put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", OpenRouterPromptBuilder.buildUserPrompt(request)),
                )
            }
    }

    private fun appendStyleExamples(
        target: JSONArray,
        styleSamples: List<StyleTrainingSample>,
    ) {
        styleSamples
            .takeLast(4)
            .forEach { sample ->
                target.put(
                    JSONObject()
                        .put(
                            "role",
                            "user",
                        )
                        .put(
                            "content",
                            "다음은 사용자가 실제로 답했던 예시다. 이 질문에 사용자는 이런 문체로 답한다.\n질문: ${sample.prompt}",
                        ),
                )
                target.put(
                    JSONObject()
                        .put("role", "assistant")
                        .put("content", sample.answer.trim()),
                )
            }
    }

    private fun parseCategory(rawCategory: String, index: Int): ReplyCategory {
        return when (rawCategory.trim().uppercase()) {
            "SAFE" -> ReplyCategory.SAFE
            "WITTY" -> ReplyCategory.WITTY
            "SWEET", "FLIRTY", "ROMANTIC" -> ReplyCategory.SWEET
            "SHORT" -> ReplyCategory.SHORT
            "FOLLOW_UP", "FOLLOWUP", "QUESTION" -> ReplyCategory.FOLLOW_UP
            else -> CATEGORY_ORDER.getOrElse(index) { ReplyCategory.SAFE }
        }
    }

    companion object {
        private const val TAG = "OpenRouterApiClient"
        private const val API_URL = "https://openrouter.ai/api/v1/chat/completions"
        private const val DEFAULT_MODEL = "openrouter/auto"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val CATEGORY_ORDER = listOf(
            ReplyCategory.SAFE,
            ReplyCategory.WITTY,
            ReplyCategory.SWEET,
            ReplyCategory.SHORT,
            ReplyCategory.FOLLOW_UP,
        )
    }
}
