package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

class VoiceAssistantManager(private val context: Context) : TextToSpeech.OnInitListener, RecognitionListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null

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
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("VoiceAssistantManager", "TTS initialization error", e)
        }
    }

    private fun initSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@VoiceAssistantManager)
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceAssistantManager", "SpeechRecognizer create error", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsReady = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
    _isSpeaking.value = false
    _audioWaveLevel.value = 0f

    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        startListening()
    }, 500)
}
                
                }
            })
        }
    }

    fun speak(text: String, pitch: Float = 1.0f, rate: Float = 1.0f) {
        if (isTtsReady && tts != null) {
            tts?.setPitch(pitch)
            tts?.setSpeechRate(rate)
            _isSpeaking.value = true
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_UTTERANCE_${System.currentTimeMillis()}")
        }
    }

    fun stopSpeaking() {
        if (isTtsReady) {
            tts?.stop()
        }
        _isSpeaking.value = false
    }

    fun startListening() {
        if (_isListening.value) return
        _recognizedText.value = ""
        _isListening.value = true

        if (speechRecognizer != null && SpeechRecognizer.isRecognitionAvailable(context)) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("VoiceAssistantManager", "Error starting speech recognizer", e)
                _isListening.value = false
            }
        } else {
            // Simulated voice pulse if speech recognizer is unavailable
            _audioWaveLevel.value = 0.8f
        }
    }

    fun stopListening() {
        _isListening.value = false
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("VoiceAssistantManager", "Error stopping speech recognizer", e)
        }
        _audioWaveLevel.value = 0f
    }

    // SpeechRecognizer listener implementation
    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {
        _isListening.value = true
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Convert rmsdB to 0.0 - 1.0 range for wave visualizer
        val normalized = ((rmsdB + 2) / 12f).coerceIn(0.1f, 1.0f)
        _audioWaveLevel.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _isListening.value = false
        _audioWaveLevel.value = 0f
    }

    override fun onError(error: Int) {
    _isListening.value = false
    _audioWaveLevel.value = 0f

    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        startListening()
    }, 800)
}

    override fun onResults(results: Bundle?) {
    _isListening.value = false
    _audioWaveLevel.value = 0f

    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
    val text = matches?.firstOrNull() ?: ""

    if (text.isNotBlank()) {
        _recognizedText.value = text
        onSpeechResultListener?.invoke(text)
    }

    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        startListening()
    }, 800)
}

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotBlank()) {
            _recognizedText.value = text
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        try {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("VoiceAssistantManager", "Destroy error", e)
        }
    }
}
