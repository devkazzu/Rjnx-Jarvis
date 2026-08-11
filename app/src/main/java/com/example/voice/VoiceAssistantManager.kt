package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * RJNX Jarvis voice engine.
 *
 * - Uses Android SpeechRecognizer for speech input.
 * - Uses Android TextToSpeech for spoken replies.
 * - Auto-listens again after a command/reply so the Voice screen behaves like
 *   an always-ready assistant while the app is in the foreground.
 * - Manual Stop disables auto-listening until the next Start action.
 */
class VoiceAssistantManager(private val context: Context) :
    TextToSpeech.OnInitListener,
    RecognitionListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val handler = Handler(Looper.getMainLooper())
    private var autoListenEnabled = true
    private var recognizerStarting = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _audioWaveLevel = MutableStateFlow(0f)
    val audioWaveLevel: StateFlow<Float> = _audioWaveLevel.asStateFlow()

    var onSpeechResultListener: ((String) -> Unit)? = null

    init {
        initTts()
        initSpeechRecognizer()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "TTS initialization error", e)
        }
    }

    private fun initSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@VoiceAssistantManager)
                }
            } else {
                Log.w(TAG, "Speech recognition is not available on this device")
            }
        } catch (e: Exception) {
            Log.e(TAG, "SpeechRecognizer create error", e)
        }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e(TAG, "TTS initialization failed: $status")
            return
        }

        val result = tts?.setLanguage(Locale.getDefault())
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.language = Locale.US
        }

        isTtsReady = true
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                handler.post {
                    _isSpeaking.value = true
                }
            }

            override fun onDone(utteranceId: String?) {
                handler.post {
                    _isSpeaking.value = false
                    _audioWaveLevel.value = 0f
                    scheduleAutoListen(350)
                }
            }

            override fun onError(utteranceId: String?) {
                handler.post {
                    _isSpeaking.value = false
                    _audioWaveLevel.value = 0f
                    scheduleAutoListen(500)
                }
            }
        })
    }

    fun speak(text: String, pitch: Float = 1.0f, rate: Float = 1.0f) {
        if (!isTtsReady || tts == null || text.isBlank()) return

        try {
            tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
            tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
            _isSpeaking.value = true
            tts?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS_UTTERANCE_${System.currentTimeMillis()}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak error", e)
            _isSpeaking.value = false
            scheduleAutoListen(500)
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "TTS stop error", e)
        }
        _isSpeaking.value = false
        _audioWaveLevel.value = 0f
    }

    /** Start foreground voice capture. Calling this also enables auto-listening. */
    fun startListening() {
        autoListenEnabled = true
        handler.removeCallbacksAndMessages(AUTO_LISTEN_TOKEN)
        startListeningInternal()
    }

    /** Stop capture and disable automatic restarts until startListening() is called. */
    fun stopListening() {
        autoListenEnabled = false
        handler.removeCallbacksAndMessages(AUTO_LISTEN_TOKEN)
        _isListening.value = false
        recognizerStarting = false
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling speech recognizer", e)
        }
        _audioWaveLevel.value = 0f
    }

    fun setAutoListenEnabled(enabled: Boolean) {
        autoListenEnabled = enabled
        if (enabled) {
            scheduleAutoListen(100)
        } else {
            handler.removeCallbacksAndMessages(AUTO_LISTEN_TOKEN)
        }
    }

    private fun startListeningInternal() {
        if (!autoListenEnabled || _isListening.value || _isSpeaking.value || recognizerStarting) return

        val recognizer = speechRecognizer ?: run {
            initSpeechRecognizer()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        _recognizedText.value = ""
        _isListening.value = true
        recognizerStarting = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            }
        }

        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognizer", e)
            recognizerStarting = false
            _isListening.value = false
            scheduleAutoListen(1200)
        }
    }

    private fun scheduleAutoListen(delayMs: Long) {
        if (!autoListenEnabled) return
        handler.removeCallbacksAndMessages(AUTO_LISTEN_TOKEN)
        handler.postAtTime({
            if (autoListenEnabled && !_isSpeaking.value && !_isListening.value) {
                startListeningInternal()
            } else if (autoListenEnabled) {
                scheduleAutoListen(700)
            }
        }, AUTO_LISTEN_TOKEN, android.os.SystemClock.uptimeMillis() + delayMs)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        recognizerStarting = false
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {
        recognizerStarting = false
        _isListening.value = true
    }

    override fun onRmsChanged(rmsdB: Float) {
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1f)
        _audioWaveLevel.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        recognizerStarting = false
        _isListening.value = false
        _audioWaveLevel.value = 0f
    }

    override fun onError(error: Int) {
        recognizerStarting = false
        _isListening.value = false
        _audioWaveLevel.value = 0f
        Log.d(TAG, "SpeechRecognizer error: $error")
        scheduleAutoListen(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1800 else 900)
    }

    override fun onResults(results: Bundle?) {
        recognizerStarting = false
        _isListening.value = false
        _audioWaveLevel.value = 0f

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()?.trim().orEmpty()

        if (text.isNotBlank()) {
            _recognizedText.value = text
            onSpeechResultListener?.invoke(text)
        } else {
            scheduleAutoListen(500)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()?.trim().orEmpty()
        if (text.isNotBlank()) _recognizedText.value = text
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    fun destroy() {
        autoListenEnabled = false
        handler.removeCallbacksAndMessages(AUTO_LISTEN_TOKEN)
        try {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Destroy error", e)
        } finally {
            tts = null
            speechRecognizer = null
        }
    }

    companion object {
        private const val TAG = "VoiceAssistantManager"
        private val AUTO_LISTEN_TOKEN = Any()
    }
}
