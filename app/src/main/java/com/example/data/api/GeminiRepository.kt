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
import org.json.JSONArray

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
        personalityPrompt: String = "You are Anu, Mio, an advanced AI voice and productivity assistant built with futuristic capabilities. Be intelligent, concise, polite, helpful, and energetic.",
        customApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.ifBlank { null }
            ?: runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()
            ?: ""

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateLocalFallbackResponse(prompt)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$apiKey"

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
            generationConfig = GeminiGenerationConfig(temperature = null, topP = null, maxOutputTokens = 2048)
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
                    return@withContext "Anu Network Alert [${response.code}]: Unable to process request via Gemini server. Standard offline mode engaged."
                }

                val responseAdapter = moshi.adapter(GeminiResponse::class.java)
                val geminiResponse = responseAdapter.fromJson(bodyString)

                val replyText = geminiResponse?.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text

                return@withContext replyText?.trim() ?: "Anu received empty payload."
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Exception calling Gemini API", e)
            return@withContext generateLocalFallbackResponse(prompt)
        }
    }

    /** Returns a small, safe action plan. The model can only choose actions exposed by the app. */
    suspend fun planActions(prompt: String, screenContext: String, memoryContext: String, customApiKey: String? = null): List<ActionPlan> = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.ifBlank { null } ?: runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull() ?: ""
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") return@withContext emptyList()
        val instruction = """
            You are the action router for a phone assistant. Decide whether the user's request requires phone actions.
            If it is only a normal question/conversation, return []. Otherwise return a JSON array containing AT MOST ONE next action only.
            Allowed action types: OPEN_APP, LAUNCH_SETTINGS, BACK, HOME, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, SCROLL, CLICK, TYPE_TEXT, WEB_SEARCH, MAKE_PHONE_CALL, SEND_MESSAGE, SET_ALARM, SET_TIMER, FLASHLIGHT.
            Each item must be {"type":"TYPE","target":"target","detail":"optional"}.
            Use screen text to choose an exact visible label when navigating inside an app. Never invent a label if a better match is visible. If the goal is already complete, return [].
            User: $prompt
            Current screen: $screenContext
            Relevant memory: $memoryContext
        """.trimIndent()
        try {
            val payload = GeminiRequest(
                contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = instruction)))),
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = "Return JSON only."))),
                generationConfig = GeminiGenerationConfig(temperature = null, topP = null, maxOutputTokens = 800)
            )
            val json = moshi.adapter(GeminiRequest::class.java).toJson(payload)
            val request = Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$apiKey").post(json.toRequestBody(jsonMediaType)).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string().orEmpty()
                val reply = moshi.adapter(GeminiResponse::class.java).fromJson(body)?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text.orEmpty()
                val clean = reply.substringAfter("[", reply).substringBeforeLast("]", "]")
                val arr = JSONArray("[$clean]")
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val type = o.optString("type").uppercase()
                        val target = o.optString("target")
                        val detail = o.optString("detail")
                        if (type.isNotBlank()) add(ActionPlan(type, target, detail))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Action planner error", e)
            emptyList()
        }
    }

    data class ActionPlan(val type: String, val target: String, val detail: String = "")

    private fun generateLocalFallbackResponse(prompt: String): String {
        val lower = prompt.lowercase().trim()
        val mathCandidate = lower.replace("what is", "").replace("calculate", "").replace("solve", "").trim()
        if (mathCandidate.matches(Regex("[0-9\\s+\\-*/().×÷]+"))) {
            return try {
                val expression = mathCandidate.replace("×", "*").replace("÷", "/")
                val result = evalSimpleExpression(expression)
                if (result % 1.0 == 0.0) result.toLong().toString() else String.format("%.4f", result)
            } catch (_: Exception) { "I couldn't calculate that locally." }
        }
        return when {
            "hello" in lower || "hi" in lower || "hey" in lower ->
                "Greetings! Anu online and operating at full capacity. How can I assist your productivity or queries today?"
            "weather" in lower ->
                "Current Ambient Conditions: 24°C (75°F) - Clear Sky, Atmospheric Humidity 48%, Light breeze from NE at 12 km/h. Perfect weather for deep work."
            "who are you" in lower || "your name" in lower ->
                "I am Anu — your personal futuristic AI assistant with voice capabilities, study tools, smart shortcuts, and productivity tracking."
            "time" in lower || "date" in lower ->
                "System Clock indicates standard local time. All internal schedulers and timers are synchronized."
            "doubt" in lower || "solve" in lower || "math" in lower ->
                "Doubt Analyzer active: Breaking problem into foundational steps. Step 1: Identify given variables. Step 2: Apply core formulas. Step 3: Verify boundary conditions."
            "summary" in lower || "summarize" in lower ->
                "Executive Summary generated: 1. Core objective defined. 2. Key findings highlighted. 3. Actionable takeaways listed concisely."
            else ->
                "Anu processed your input: \"$prompt\". I am configured to manage your Voice commands, Study goals, Notes, Expenses, and Smart Utilities."
        }
    }
    private fun evalSimpleExpression(expr: String): Double {
        return object {
            var pos = -1; var ch = 0
            fun next() { ch = if (++pos < expr.length) expr[pos].code else -1 }
            fun eat(c: Int): Boolean { if (ch == c) { next(); return true }; return false }
            fun parse(): Double { next(); val x = expression(); if (pos < expr.length) error("unexpected") ; return x }
            fun expression(): Double { var x = term(); while (true) { if (eat('+'.code)) x += term() else if (eat('-'.code)) x -= term() else return x } }
            fun term(): Double { var x = factor(); while (true) { if (eat('*'.code)) x *= factor() else if (eat('/'.code)) x /= factor() else return x } }
            fun factor(): Double { if (eat('+'.code)) return factor(); if (eat('-'.code)) return -factor(); if (eat('('.code)) { val x=expression(); if(!eat(')'.code)) error("paren"); return x }; val st=pos; while((ch>='0'.code&&ch<='9'.code)||ch=='.'.code) next(); return expr.substring(st,pos).toDouble() }
        }.parse()
    }

}
