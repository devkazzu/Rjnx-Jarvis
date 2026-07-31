package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jarvis_settings")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_AI_PERSONALITY = stringPreferencesKey("ai_personality")
        val KEY_AI_PERSONALITY_TONE = stringPreferencesKey("ai_personality_tone")
        val KEY_AI_RESPONSE_VERBOSITY = stringPreferencesKey("ai_response_verbosity")
        val KEY_AI_CUSTOM_DIRECTIVE = stringPreferencesKey("ai_custom_directive")
        val KEY_CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        val KEY_VOICE_OUTPUT_ENABLED = booleanPreferencesKey("voice_output_enabled")
        val KEY_WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val KEY_SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        val KEY_BACKGROUND_SERVICE_ENABLED = booleanPreferencesKey("background_service_enabled")
        val KEY_FLOATING_BUTTON_ENABLED = booleanPreferencesKey("floating_button_enabled")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "Dark Futuristic"
    }

    val aiPersonality: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_PERSONALITY] ?: "Classic Jarvis"
    }

    val aiPersonalityTone: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_PERSONALITY_TONE] ?: "Classic Futuristic"
    }

    val aiResponseVerbosity: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_RESPONSE_VERBOSITY] ?: "Balanced"
    }

    val aiCustomDirective: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_CUSTOM_DIRECTIVE] ?: "Always be polite, highly intelligent, and structured."
    }

    val customApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_API_KEY] ?: ""
    }

    val isVoiceOutputEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOICE_OUTPUT_ENABLED] ?: true
    }

    val isWakeWordEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WAKE_WORD_ENABLED] ?: true
    }

    val speechPitch: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPEECH_PITCH] ?: 1.0f
    }

    val speechRate: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPEECH_RATE] ?: 1.0f
    }

    val accentColor: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACCENT_COLOR] ?: "Cyan"
    }

    val isBackgroundServiceEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BACKGROUND_SERVICE_ENABLED] ?: false
    }

    val isFloatingButtonEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FLOATING_BUTTON_ENABLED] ?: false
    }

    suspend fun updateThemeMode(theme: String) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = theme }
    }

    suspend fun updateAiPersonality(personality: String) {
        context.dataStore.edit { prefs -> prefs[KEY_AI_PERSONALITY] = personality }
    }

    suspend fun updateAiPersonalityTone(tone: String) {
        context.dataStore.edit { prefs -> prefs[KEY_AI_PERSONALITY_TONE] = tone }
    }

    suspend fun updateAiResponseVerbosity(verbosity: String) {
        context.dataStore.edit { prefs -> prefs[KEY_AI_RESPONSE_VERBOSITY] = verbosity }
    }

    suspend fun updateAiCustomDirective(directive: String) {
        context.dataStore.edit { prefs -> prefs[KEY_AI_CUSTOM_DIRECTIVE] = directive }
    }

    suspend fun updateCustomApiKey(key: String) {
        context.dataStore.edit { prefs -> prefs[KEY_CUSTOM_API_KEY] = key }
    }

    suspend fun updateVoiceOutputEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_VOICE_OUTPUT_ENABLED] = enabled }
    }

    suspend fun updateWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun updateSpeechPitch(pitch: Float) {
        context.dataStore.edit { prefs -> prefs[KEY_SPEECH_PITCH] = pitch }
    }

    suspend fun updateSpeechRate(rate: Float) {
        context.dataStore.edit { prefs -> prefs[KEY_SPEECH_RATE] = rate }
    }

    suspend fun updateAccentColor(colorName: String) {
        context.dataStore.edit { prefs -> prefs[KEY_ACCENT_COLOR] = colorName }
    }

    suspend fun updateBackgroundServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_BACKGROUND_SERVICE_ENABLED] = enabled }
    }

    suspend fun updateFloatingButtonEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_FLOATING_BUTTON_ENABLED] = enabled }
    }
}
