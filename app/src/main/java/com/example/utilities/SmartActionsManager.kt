package com.example.utilities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log

object SmartActionsManager {

    data class ActionRequest(
        val type: ActionType,
        val target: String,
        val detail: String = ""
    )

    enum class ActionType {
    ANSWER_CALL,
    END_CALL,
    SPEAKER_ON,
    SPEAKER_OFF,
    MUTE_CALL,
    UNMUTE_CALL,
    BLUETOOTH_SETTINGS,
    OPEN_APP,
    LAUNCH_SETTINGS,
    SET_ALARM,
    SET_TIMER,
    MAKE_PHONE_CALL,
    SEND_MESSAGE,
    WEB_SEARCH,
    SCREEN_EXPLAIN,
    FLASHLIGHT
}
    // Parses natural command to check if it matches a system smart action
    fun parseCommand(command: String): ActionRequest? {
        val lower = command.lowercase().trim()

        return when {
            lower.contains("answer call") || lower.contains("pick up the call") || lower == "answer" -> {
                ActionRequest(ActionType.ANSWER_CALL, "")
            }

            lower.contains("reject call") || lower.contains("decline call") || lower.contains("cut call") || lower.contains("end call") || lower == "hang up" -> {
                ActionRequest(ActionType.END_CALL, "")
            }

            lower.contains("speaker on") || lower.contains("turn speaker on") || lower.contains("speakerphone on") -> {
                ActionRequest(ActionType.SPEAKER_ON, "")
            }

            lower.contains("speaker off") || lower.contains("turn speaker off") || lower.contains("speakerphone off") -> {
                ActionRequest(ActionType.SPEAKER_OFF, "")
            }

            lower.contains("mute call") || lower == "mute" || lower.contains("microphone mute") -> {
                ActionRequest(ActionType.MUTE_CALL, "")
            }

            lower.contains("unmute call") || lower == "unmute" || lower.contains("microphone unmute") -> {
                ActionRequest(ActionType.UNMUTE_CALL, "")
            }

            lower.contains("bluetooth settings") || lower.contains("bluetooth on") || lower.contains("bluetooth off") || lower.contains("connect bluetooth") -> {
                ActionRequest(ActionType.BLUETOOTH_SETTINGS, "")
            }

            lower.startsWith("open app") || lower.startsWith("open ") || lower.startsWith("launch ") -> {
                val appName = lower.replace("open app ", "")
                    .replace("open ", "")
                    .replace("launch ", "")
                    .trim()
                ActionRequest(ActionType.OPEN_APP, appName)
            }

            lower.contains("open settings") || lower.contains("wifi settings") || lower.contains("bluetooth settings") -> {
                ActionRequest(ActionType.LAUNCH_SETTINGS, lower)
            }

            lower.startsWith("set alarm") || lower.contains("alarm for") -> {
                ActionRequest(ActionType.SET_ALARM, lower)
            }

            lower.startsWith("set timer") || lower.contains("timer for") -> {
                ActionRequest(ActionType.SET_TIMER, lower)
            }

            lower.startsWith("call ") || lower.startsWith("phone call ") || lower.startsWith("dial ") -> {
                val numberOrName = lower.replace("call ", "").replace("phone call ", "").replace("dial ", "").trim()
                ActionRequest(ActionType.MAKE_PHONE_CALL, numberOrName)
            }

            lower.startsWith("send message ") || lower.startsWith("sms ") || lower.startsWith("text ") -> {
                ActionRequest(ActionType.SEND_MESSAGE, lower)
            }

            lower.startsWith("search ") || lower.startsWith("google ") -> {
                val query = lower.replace("search ", "").replace("google ", "").trim()
                ActionRequest(ActionType.WEB_SEARCH, query)
            }

            lower.contains("read screen") || lower.contains("what is on my screen") || lower.contains("summarize screen") -> {
                ActionRequest(ActionType.SCREEN_EXPLAIN, lower)
            }

            lower.contains("turn on flashlight") ||
lower.contains("flashlight on") ||
lower.contains("torch on") -> {
    ActionRequest(ActionType.FLASHLIGHT, "on")
}

lower.contains("turn off flashlight") ||
lower.contains("flashlight off") ||
lower.contains("torch off") -> {
    ActionRequest(ActionType.FLASHLIGHT, "off")
}
            else -> null
        }
    }

    fun executeAction(context: Context, request: ActionRequest): String {
        return try {
            when (request.type) {
                ActionType.ANSWER_CALL -> CallControlManager.answer(context)
                ActionType.END_CALL -> CallControlManager.end(context)
                ActionType.SPEAKER_ON -> CallControlManager.setSpeaker(context, true)
                ActionType.SPEAKER_OFF -> CallControlManager.setSpeaker(context, false)
                ActionType.MUTE_CALL -> CallControlManager.setMute(context, true)
                ActionType.UNMUTE_CALL -> CallControlManager.setMute(context, false)
                ActionType.BLUETOOTH_SETTINGS -> CallControlManager.openBluetoothSettings(context)
                ActionType.OPEN_APP -> openAppByName(context, request.target)
                ActionType.LAUNCH_SETTINGS -> launchSettings(context, request.target)
                ActionType.SET_ALARM -> setAlarmFromText(context, request.target)
                ActionType.SET_TIMER -> setTimerFromText(context, request.target)
                ActionType.MAKE_PHONE_CALL -> initiatePhoneCall(context, request.target)
                ActionType.SEND_MESSAGE -> initiateSendMessage(context, request.target)
                ActionType.WEB_SEARCH -> {
                    SystemUtils.performGoogleSearch(context, request.target)
                    "Searching Google for '${request.target}'"
                }
                ActionType.SCREEN_EXPLAIN -> "Analyzing screen content..."
                ActionType.FLASHLIGHT -> toggleFlashlight(context, request.target)
            }
        } catch (e: Exception) {
            Log.e("SmartActionsManager", "Error executing action", e)
            "Unable to complete command: ${e.localizedMessage}"
        }
    }

    private fun openAppByName(context: Context, target: String): String {
        val installed = SystemUtils.getInstalledApps(context)
        val matched = installed.firstOrNull { it.name.lowercase().contains(target) || target.contains(it.name.lowercase()) }
        return if (matched != null) {
            val success = SystemUtils.launchApp(context, matched.packageName)
            if (success) "Opening ${matched.name}..." else "Failed to open ${matched.name}"
        } else {
            // Common default fallback packages
            val pkg = when (target) {
                "youtube" -> "com.google.android.youtube"
                "chrome" -> "com.android.chrome"
                "whatsapp" -> "com.whatsapp"
                "maps" -> "com.google.android.apps.maps"
                "camera" -> "com.android.camera"
                "settings" -> "com.android.settings"
                else -> null
            }
            if (pkg != null && SystemUtils.launchApp(context, pkg)) {
                "Opening $target..."
            } else {
                "App '$target' not found on device."
            }
        }
    }

    private fun launchSettings(context: Context, target: String): String {
        val intent = when {
            target.contains("wifi") -> Intent(Settings.ACTION_WIFI_SETTINGS)
            target.contains("bluetooth") -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            target.contains("accessibility") -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            target.contains("battery") -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        context.startActivity(intent)
        return "Opening Settings..."
    }

    private fun setAlarmFromText(context: Context, text: String): String {
        // Extract numbers for hour and minute if present
        val numbers = Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()
        val hour = numbers.getOrNull(0) ?: 7
        val minute = numbers.getOrNull(1) ?: 0
        SystemUtils.setAlarm(context, hour, minute, "Jarvis Alarm")
        return "Setting alarm for ${String.format("%02d:%02d", hour, minute)}"
    }

    private fun setTimerFromText(context: Context, text: String): String {
        val number = Regex("\\d+").find(text)?.value?.toInt() ?: 60
        SystemUtils.setTimer(context, number, "Jarvis Timer")
        return "Setting timer for $number seconds"
    }

    private fun initiatePhoneCall(context: Context, target: String): String {
        val cleanNumber = target.filter { it.isDigit() || it == '+' }
        if (cleanNumber.length >= 3) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return "Dialing $cleanNumber..."
        }
        return "To make a phone call, please specify a valid phone number."
    }

    private fun initiateSendMessage(context: Context, target: String): String {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
            putExtra("sms_body", "Sent via RJNX Jarvis Assistant")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Opening message composer..."
    }
private fun toggleFlashlight(context: Context, mode: String): String {
    return try {
        val manager = context.getSystemService(Context.CAMERA_SERVICE)
                as android.hardware.camera2.CameraManager

        val id = manager.cameraIdList.first()

        manager.setTorchMode(id, mode == "on")

        if (mode == "on")
            "Flashlight turned on."
        else
            "Flashlight turned off."

    } catch (e: Exception) {
        "Flashlight not supported."
    }
}}
