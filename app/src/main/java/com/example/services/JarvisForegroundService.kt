package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class JarvisForegroundService : Service(), RecognitionListener {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningWakeWord = false

    companion object {
        const val CHANNEL_ID = "jarvis_foreground_channel"
        const val NOTIFICATION_ID = 1001

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _lastDetectedWakeWordTime = MutableStateFlow(0L)
        val lastDetectedWakeWordTime: StateFlow<Long> = _lastDetectedWakeWordTime.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, JarvisForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, JarvisForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("RJNX Jarvis Active - Listening for 'Hey Jarvis'"))
        _isRunning.value = true

        initWakeWordRecognizer()
        startWakeWordListening()
        Log.d("JarvisService", "Jarvis Foreground Service Started")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RJNX Jarvis Foreground Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Jarvis running in background for wake word and smart tasks"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RJNX Jarvis Assistant")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun initWakeWordRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(this@JarvisForegroundService)
                }
            }
        } catch (e: Exception) {
            Log.e("JarvisService", "SpeechRecognizer error", e)
        }
    }

    private fun startWakeWordListening() {
        if (speechRecognizer == null || isListeningWakeWord) return
        isListeningWakeWord = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("JarvisService", "Start listening error", e)
        }
    }

    override fun onResults(results: android.os.Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val spokenText = matches?.firstOrNull()?.lowercase() ?: ""
        Log.d("JarvisService", "Background heard: $spokenText")

        isListeningWakeWord = false

        if (spokenText.contains("hey jarvis") || spokenText.contains("jarvis") || spokenText.contains("hey assistant")) {
            _lastDetectedWakeWordTime.value = System.currentTimeMillis()
            // Launch main app or trigger voice prompt
            val appIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("TRIGGER_VOICE_PROMPT", true)
            }
            startActivity(appIntent)
        }

        // Restart wake word loop after delay
        serviceScope.launch {
            delay(1500)
            if (_isRunning.value) {
                startWakeWordListening()
            }
        }
    }

    override fun onError(error: Int) {
        isListeningWakeWord = false
        // Automatically restart listening on silent errors or timeout
        serviceScope.launch {
            delay(2000)
            if (_isRunning.value) {
                startWakeWordListening()
            }
        }
    }

    override fun onReadyForSpeech(params: android.os.Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: android.os.Bundle?) {}
    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("JarvisService", "Destroy error", e)
        }
        Log.d("JarvisService", "Jarvis Foreground Service Stopped")
    }
}
