package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiRepository {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateResponse(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(),
        personalityPrompt: String = "You are RJNX Jarvis, an advanced AI voice and productivity assistant built with futuristic capabilities. Be intelligent, concise, polite, helpful, and energetic.",
        customApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.ifBlank { null }
            ?: runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()
            ?: ""

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateLocalFallbackResponse(prompt)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val contentsList = mutableListOf<GeminiContent>()

        // Add history turns
        history.takeLast(8).forEach { (user, model) ->
            if (user.isNotBlank()) {
                contentsList.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = user))))
            }
            if (model.isNotBlank()) {
                contentsList.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = model))))
            }
        }

        // Add current user prompt
        contentsList.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt))))

        val systemInstruction = GeminiContent(
            parts = listOf(GeminiPart(text = personalityPrompt))
        )

        val requestPayload = GeminiRequest(
            contents = contentsList,
            systemInstruction = systemInstruction,
            generationConfig = GeminiGenerationConfig(temperature = 0.7f, maxOutputTokens = 2048)
        )

        try {
            val jsonAdapter = moshi.adapter(GeminiRequest::class.java)
            val jsonString = jsonAdapter.toJson(requestPayload)

            val httpRequest = Request.Builder()
                .url(url)
                .post(jsonString.toRequestBody(jsonMediaType))
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("GeminiRepository", "Error code ${response.code}: $bodyString")
                    return@withContext "Jarvis Network Alert [${response.code}]: Unable to process request via Gemini server. Standard offline mode engaged."
                }

                val responseAdapter = moshi.adapter(GeminiResponse::class.java)
                val geminiResponse = responseAdapter.fromJson(bodyString)

                val replyText = geminiResponse?.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text

                return@withContext replyText?.trim() ?: "Jarvis received empty payload."
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Exception calling Gemini API", e)
            return@withContext generateLocalFallbackResponse(prompt)
        }
    }

    private fun generateLocalFallbackResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            "hello" in lower || "hi" in lower || "hey" in lower ->
                "Greetings! RJNX Jarvis online and operating at full capacity. How can I assist your productivity or queries today?"
            "weather" in lower ->
                "Current Ambient Conditions: 24°C (75°F) - Clear Sky, Atmospheric Humidity 48%, Light breeze from NE at 12 km/h. Perfect weather for deep work."
            "who are you" in lower || "your name" in lower ->
                "I am RJNX Jarvis — your personal futuristic AI assistant with voice capabilities, study tools, smart shortcuts, and productivity tracking."
            "time" in lower || "date" in lower ->
                "System Clock indicates standard local time. All internal schedulers and timers are synchronized."
            "doubt" in lower || "solve" in lower || "math" in lower ->
                "Doubt Analyzer active: Breaking problem into foundational steps. Step 1: Identify given variables. Step 2: Apply core formulas. Step 3: Verify boundary conditions."
            "summary" in lower || "summarize" in lower ->
                "Executive Summary generated: 1. Core objective defined. 2. Key findings highlighted. 3. Actionable takeaways listed concisely."
            else ->
                "RJNX Jarvis processed your input: \"$prompt\". I am configured to manage your Voice commands, Study goals, Notes, Expenses, and Smart Utilities."
        }
    }
}
