package com.example.utilities

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log

/** Basic hands-free phone-call controls available to a normal Android app. */
object CallControlManager {

    fun answer(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val telecom = context.getSystemService(TelecomManager::class.java)
                telecom?.acceptRingingCall()
                "Answering the call."
            } else {
                "Call answering requires Android 8.0 or newer."
            }
        } catch (e: SecurityException) {
            "I need Answer Phone Calls permission to answer the call."
        } catch (e: Exception) {
            Log.e(TAG, "Answer call failed", e)
            "I could not answer the call on this device."
        }
    }

    fun end(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val telecom = context.getSystemService(TelecomManager::class.java)
                telecom?.endCall()
                "Ending the call."
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val telecom = context.getSystemService(TelecomManager::class.java)
                telecom?.endCall()
                "Ending the call."
            } else {
                "Call ending requires Android 8.0 or newer."
            }
        } catch (e: SecurityException) {
            "I need Answer Phone Calls permission to end the call."
        } catch (e: Exception) {
            Log.e(TAG, "End call failed", e)
            "I could not end the call on this device."
        }
    }

    fun setSpeaker(context: Context, enabled: Boolean): String {
        return try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            @Suppress("DEPRECATION")
            audio.isSpeakerphoneOn = enabled
            if (enabled) "Speakerphone turned on." else "Speakerphone turned off."
        } catch (e: Exception) {
            "Speaker control is not available during this call."
        }
    }

    fun setMute(context: Context, muted: Boolean): String {
        return try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            @Suppress("DEPRECATION")
            audio.isMicrophoneMute = muted
            if (muted) "Microphone muted." else "Microphone unmuted."
        } catch (e: Exception) {
            "Microphone mute control is not available."
        }
    }

    fun openBluetoothSettings(context: Context): String {
        return try {
            context.startActivity(
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            "Opening Bluetooth settings."
        } catch (e: Exception) {
            "Bluetooth settings are not available."
        }
    }

    private const val TAG = "CallControlManager"
}
