package com.example.utilities

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.AlarmClock
import android.util.Log

object SystemUtils {

    // App Launcher
    data class AppInfo(val name: String, val packageName: String)

    fun getInstalledApps(context: Context): List<AppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
        return resolveInfoList.mapNotNull { resolveInfo ->
            val appName = resolveInfo.loadLabel(packageManager).toString()
            val pkgName = resolveInfo.activityInfo.packageName
            if (pkgName != context.packageName) {
                AppInfo(appName, pkgName)
            } else null
        }.sortedBy { it.name }
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else false
        } catch (e: Exception) {
            Log.e("SystemUtils", "Error launching app: $packageName", e)
            false
        }
    }

    // Set Alarm
    fun setAlarm(context: Context, hour: Int, minute: Int, message: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    // Set Timer
    fun setTimer(context: Context, seconds: Int, message: String) {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    // Web Search & Website Opener
    fun openWebsite(context: Context, url: String) {
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else url
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun performGoogleSearch(context: Context, query: String) {
        val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
        openWebsite(context, searchUrl)
    }

    // Flashlight Controller
    fun toggleFlashlight(context: Context, enabled: Boolean): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enabled)
                true
            } else false
        } catch (e: Exception) {
            Log.e("SystemUtils", "Flashlight error", e)
            false
        }
    }

    // Calculator Engine
    fun evaluateMathExpression(expression: String): String {
        return try {
            val clean = expression.replace("×", "*").replace("÷", "/").replace(" ", "")
            val result = evalSimpleExpr(clean)
            if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                String.format("%.4f", result)
            }
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun evalSimpleExpr(expr: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < expr.length) expr[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expr.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> x /= parseFactor()
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = expr.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                return x
            }
        }.parse()
    }

    // Unit Converter
    fun convertUnit(value: Double, category: String, fromUnit: String, toUnit: String): String {
        return try {
            val result = when (category) {
                "Length" -> convertLength(value, fromUnit, toUnit)
                "Mass" -> convertMass(value, fromUnit, toUnit)
                "Temperature" -> convertTemperature(value, fromUnit, toUnit)
                "Speed" -> convertSpeed(value, fromUnit, toUnit)
                else -> value
            }
            if (result % 1.0 == 0.0) result.toLong().toString() else String.format("%.3f", result)
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun convertLength(valIn: Double, from: String, to: String): Double {
        val meters = when (from) {
            "Meter" -> valIn
            "Kilometer" -> valIn * 1000
            "Centimeter" -> valIn / 100
            "Mile" -> valIn * 1609.34
            "Foot" -> valIn * 0.3048
            "Inch" -> valIn * 0.0254
            else -> valIn
        }
        return when (to) {
            "Meter" -> meters
            "Kilometer" -> meters / 1000
            "Centimeter" -> meters * 100
            "Mile" -> meters / 1609.34
            "Foot" -> meters / 0.3048
            "Inch" -> meters / 0.0254
            else -> meters
        }
    }

    private fun convertMass(valIn: Double, from: String, to: String): Double {
        val grams = when (from) {
            "Kilogram" -> valIn * 1000
            "Gram" -> valIn
            "Pound" -> valIn * 453.592
            "Ounce" -> valIn * 28.3495
            else -> valIn
        }
        return when (to) {
            "Kilogram" -> grams / 1000
            "Gram" -> grams
            "Pound" -> grams / 453.592
            "Ounce" -> grams / 28.3495
            else -> grams
        }
    }

    private fun convertTemperature(valIn: Double, from: String, to: String): Double {
        val celsius = when (from) {
            "Celsius" -> valIn
            "Fahrenheit" -> (valIn - 32) * 5 / 9
            "Kelvin" -> valIn - 273.15
            else -> valIn
        }
        return when (to) {
            "Celsius" -> celsius
            "Fahrenheit" -> (celsius * 9 / 5) + 32
            "Kelvin" -> celsius + 273.15
            else -> celsius
        }
    }

    private fun convertSpeed(valIn: Double, from: String, to: String): Double {
        val kmh = when (from) {
            "km/h" -> valIn
            "mph" -> valIn * 1.60934
            "m/s" -> valIn * 3.6
            else -> valIn
        }
        return when (to) {
            "km/h" -> kmh
            "mph" -> kmh / 1.60934
            "m/s" -> kmh / 3.6
            else -> kmh
        }
    }
}
