package com.example.utilities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.provider.ContactsContract
import com.example.accessibility.JarvisAccessibilityService
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
    FLASHLIGHT,
    HOME,
    BACK,
    RECENTS,
    NOTIFICATIONS,
    QUICK_SETTINGS,
    SCROLL,
    CLICK,
    TYPE_TEXT
}
    // Parses natural command to check if it matches a system smart action
    fun parseCommand(command: String): ActionRequest? {
        val lower = command.lowercase().trim().replace(Regex("\\s+"), " ")

        return when {
            lower.matches(Regex("(go|take|return|move) to (the )?(home|home screen)")) ||
                lower == "home" || lower == "go home" || lower == "home screen" -> ActionRequest(ActionType.HOME, "")
            lower == "back" || lower.contains("go back") || lower.contains("press back") -> ActionRequest(ActionType.BACK, "")
            lower.contains("recent apps") || lower.contains("open recents") || lower.contains("recent applications") -> ActionRequest(ActionType.RECENTS, "")
            lower.contains("notification panel") || lower.contains("open notifications") || lower.contains("notification shade") -> ActionRequest(ActionType.NOTIFICATIONS, "")
            lower.contains("quick settings") || lower.contains("quick panel") -> ActionRequest(ActionType.QUICK_SETTINGS, "")

            lower.contains("answer call") || lower.contains("pick up the call") || lower == "answer" -> ActionRequest(ActionType.ANSWER_CALL, "")
            lower.contains("reject call") || lower.contains("decline call") || lower.contains("cut call") || lower.contains("end call") || lower == "hang up" -> ActionRequest(ActionType.END_CALL, "")
            lower.contains("speaker on") || lower.contains("turn speaker on") || lower.contains("speakerphone on") -> ActionRequest(ActionType.SPEAKER_ON, "")
            lower.contains("speaker off") || lower.contains("turn speaker off") || lower.contains("speakerphone off") -> ActionRequest(ActionType.SPEAKER_OFF, "")
            lower.contains("mute call") || lower == "mute" || lower.contains("microphone mute") -> ActionRequest(ActionType.MUTE_CALL, "")
            lower.contains("unmute call") || lower == "unmute" || lower.contains("microphone unmute") -> ActionRequest(ActionType.UNMUTE_CALL, "")
            lower.contains("bluetooth settings") || lower.contains("bluetooth on") || lower.contains("bluetooth off") || lower.contains("connect bluetooth") -> ActionRequest(ActionType.BLUETOOTH_SETTINGS, "")

            lower.contains("open settings") || lower.contains("wifi settings") || lower.contains("message settings") || lower.contains("sms settings") ||
                lower.contains("open accessibility") || lower.contains("open display") || lower.contains("open connections") ||
                lower.contains("open connection") || lower.contains("open battery settings") || lower.contains("open notification settings") ||
                lower.contains("open sound settings") -> ActionRequest(ActionType.LAUNCH_SETTINGS, lower)

            lower.startsWith("open app") || lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ") -> {
                val appName = lower.replaceFirst(Regex("^(open app|open|launch|start)\\s+"), "").trim()
                ActionRequest(ActionType.OPEN_APP, appName)
            }
            lower.startsWith("set alarm") || lower.contains("alarm for") -> ActionRequest(ActionType.SET_ALARM, lower)
            lower.startsWith("set timer") || lower.contains("timer for") -> ActionRequest(ActionType.SET_TIMER, lower)

            lower.startsWith("call ") || lower.startsWith("phone call ") || lower.startsWith("dial ") ||
                lower.startsWith("make a call to ") || lower.startsWith("call my ") -> {
                val target = lower
                    .replaceFirst(Regex("^(make a call to|phone call|call my|call|dial)\\s+"), "")
                    .trim()
                ActionRequest(ActionType.MAKE_PHONE_CALL, target)
            }

            lower.startsWith("send message ") || lower.startsWith("sms ") || lower.startsWith("text ") -> ActionRequest(ActionType.SEND_MESSAGE, lower)
            lower.startsWith("search ") || lower.startsWith("google ") || lower.startsWith("search google for ") -> {
                val query = lower.replaceFirst(Regex("^(search google for|search|google)\\s*"), "").trim()
                ActionRequest(ActionType.WEB_SEARCH, query)
            }
            lower.contains("read screen") || lower.contains("what is on my screen") || lower.contains("summarize screen") -> ActionRequest(ActionType.SCREEN_EXPLAIN, lower)

            lower.contains("scroll") -> ActionRequest(ActionType.SCROLL, if (lower.contains("up")) "up" else "down")
            lower.startsWith("click ") || lower.startsWith("tap ") || lower.startsWith("press ") -> {
                val target = lower.replaceFirst(Regex("^(click|tap|press)\\s+"), "").trim()
                ActionRequest(ActionType.CLICK, target)
            }
            lower.startsWith("type ") || lower.startsWith("write ") -> {
                val text = lower.replaceFirst(Regex("^(type|write)\\s+"), "").trim()
                ActionRequest(ActionType.TYPE_TEXT, text)
            }
            lower.contains("turn on flashlight") || lower.contains("flashlight on") || lower.contains("torch on") -> ActionRequest(ActionType.FLASHLIGHT, "on")
            lower.contains("turn off flashlight") || lower.contains("flashlight off") || lower.contains("torch off") -> ActionRequest(ActionType.FLASHLIGHT, "off")
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
                ActionType.HOME -> globalUi("home", "Going to Home screen.")
                ActionType.BACK -> globalUi("back", "Going back.")
                ActionType.RECENTS -> globalUi("recents", "Opening recent apps.")
                ActionType.NOTIFICATIONS -> globalUi("notifications", "Opening notifications.")
                ActionType.QUICK_SETTINGS -> globalUi("quick_settings", "Opening Quick Settings.")
                ActionType.SCROLL -> if (JarvisAccessibilityService.scroll(request.target)) "Scrolling ${request.target}." else accessibilityUnavailable()
                ActionType.CLICK -> if (JarvisAccessibilityService.click(request.target)) "Tapped ${request.target}." else "I couldn't find a clickable '${request.target}' on the current screen."
                ActionType.TYPE_TEXT -> if (JarvisAccessibilityService.typeText(request.target)) "Typed it." else "I couldn't find an active text field."
            }
        } catch (e: Exception) {
            Log.e("SmartActionsManager", "Error executing action", e)
            "Unable to complete command: ${e.localizedMessage}"
        }
    }

    private fun openAppByName(context: Context, rawTarget: String): String {
        val target = rawTarget.lowercase().trim()
            .replace(Regex("^(the|my)\\s+"), "")
            .replace(Regex("\\s+(app|application)$"), "")
            .trim()

        // Prefer the real installed-app list, but also use well-known package aliases.
        val installed = SystemUtils.getInstalledApps(context)
        val aliases = mapOf(
            "instagram" to listOf("com.instagram.android"),
            "insta" to listOf("com.instagram.android"),
            "youtube" to listOf("com.google.android.youtube"),
            "whatsapp" to listOf("com.whatsapp"),
            "chrome" to listOf("com.android.chrome"),
            "google chrome" to listOf("com.android.chrome"),
            "facebook" to listOf("com.facebook.katana"),
            "telegram" to listOf("org.telegram.messenger"),
            "maps" to listOf("com.google.android.apps.maps"),
            "google maps" to listOf("com.google.android.apps.maps"),
            "photos" to listOf("com.google.android.apps.photos"),
            "gallery" to listOf("com.google.android.apps.photos", "com.sec.android.gallery3d"),
            "camera" to listOf("com.sec.android.app.camera", "com.android.camera2", "com.android.camera"),
            "settings" to listOf("com.android.settings")
        )

        val matched = installed.firstOrNull {
            val name = it.name.lowercase().trim()
            name == target || name.contains(target) || target.contains(name)
        }
        if (matched != null && SystemUtils.launchApp(context, matched.packageName)) {
            return "Opening ${matched.name}..."
        }

        for (pkg in aliases[target].orEmpty()) {
            if (SystemUtils.launchApp(context, pkg)) return "Opening $target..."
        }
        return "App '$rawTarget' not found on device."
    }

    private fun launchSettings(context: Context, target: String): String {
        val intent = when {
            target.contains("wifi") || target.contains("internet") -> Intent(Settings.ACTION_WIFI_SETTINGS)
            target.contains("bluetooth") -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            target.contains("accessibility") -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            target.contains("display") -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            target.contains("battery") -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            target.contains("notification") -> Intent(Settings.ACTION_NOTIFICATION_SETTINGS)
            target.contains("sound") -> Intent(Settings.ACTION_SOUND_SETTINGS)
            target.contains("connection") || target.contains("connections") || target.contains("network") -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
            target.contains("message") || target.contains("sms") -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.android.mms"))
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
        SystemUtils.setAlarm(context, hour, minute, "Anu Alarm")
        return "Setting alarm for ${String.format("%02d:%02d", hour, minute)}"
    }

    private fun setTimerFromText(context: Context, text: String): String {
        val number = Regex("\\d+").find(text)?.value?.toInt() ?: 60
        SystemUtils.setTimer(context, number, "Anu Timer")
        return "Setting timer for $number seconds"
    }

    private fun initiatePhoneCall(context: Context, target: String): String {
        val cleanNumber = target.filter { it.isDigit() || it == '+' }
        val number = if (cleanNumber.length >= 3) cleanNumber else findContactNumber(context, target)
        if (number.isNullOrBlank()) return "I couldn't find a contact or valid number for '$target'."

        return try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                context.startActivity(intent)
                "Calling $target..."
            } else {
                val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(dial)
                "I opened the dialer for $target. Grant Call Phone permission for direct calling."
            }
        } catch (e: Exception) {
            Log.e("SmartActionsManager", "Call failed", e)
            "Unable to start the call."
        }
    }

    private fun findContactNumber(context: Context, name: String): String? {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return null
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    private fun globalUi(action: String, success: String): String {
        return if (JarvisAccessibilityService.performGlobal(action)) success
        else "Accessibility control is unavailable. Please enable Anu in Android Accessibility settings."
    }

    private fun accessibilityUnavailable(): String =
        "Accessibility control is unavailable. Please enable Anu in Android Accessibility settings."

    private fun initiateSendMessage(context: Context, target: String): String {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
            putExtra("sms_body", "Sent via Anu Assistant")
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
