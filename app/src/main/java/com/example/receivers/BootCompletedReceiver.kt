package com.example.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.preferences.UserPreferencesRepository
import com.example.services.JarvisForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device boot completed, checking Jarvis auto-start preferences...")
            val repo = UserPreferencesRepository(context)
            CoroutineScope(Dispatchers.IO).launch {
                val isBgEnabled = repo.isBackgroundServiceEnabled.first()
                if (isBgEnabled) {
                    Log.d("BootReceiver", "Starting Jarvis Foreground Service after reboot")
                    JarvisForegroundService.startService(context)
                }
            }
        }
    }
}
