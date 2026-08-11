package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import java.util.Locale

/**
 * Gives Jarvis a controlled UI automation layer. The service can inspect the
 * current accessibility tree and perform safe UI/global actions requested by
 * the user. It never bypasses Android's accessibility/security boundaries.
 */
class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        ScreenContentHolder.setServiceActive(true)
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkgName = event.packageName?.toString() ?: return
        if (pkgName.isEmpty() || pkgName == packageName) return
        val rootNode = rootInActiveWindow ?: return
        try {
            val textBuilder = StringBuilder()
            traverseNode(rootNode, textBuilder, 0)
            val fullText = textBuilder.toString().trim()
            ScreenContentHolder.updateScreenContent(fullText, pkgName)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing screen content", e)
        } finally {
            rootNode.recycle()
        }
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, builder: StringBuilder, depth: Int) {
        if (node == null || depth > 16) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        if (!text.isNullOrBlank()) builder.append(text).append('\n')
        else if (!desc.isNullOrBlank()) builder.append('[').append(desc).append("]\n")
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                traverseNode(child, builder, depth + 1)
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
    Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        instance = null
        ScreenContentHolder.setServiceActive(false)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "JarvisAccessibility"
        @Volatile private var instance: JarvisAccessibilityService? = null

        fun isAvailable(): Boolean = instance != null

        fun performGlobal(action: String): Boolean {
            val service = instance ?: return false
            return when (action.lowercase(Locale.US)) {
                "home" -> service.performGlobalAction(GLOBAL_ACTION_HOME)
                "back" -> service.performGlobalAction(GLOBAL_ACTION_BACK)
                "recents", "recent", "recent_apps" -> service.performGlobalAction(GLOBAL_ACTION_RECENTS)
                "notifications", "notification_shade" -> service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                "quick_settings" -> service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                "power" -> service.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
                else -> false
            }
        }

        fun click(textOrDescription: String): Boolean {
            val service = instance ?: return false
            val root = service.rootInActiveWindow ?: return false
            return try {
                val node = findNode(root, textOrDescription)
                node?.let { clickNodeOrParent(it) } ?: false
            } finally { root.recycle() }
        }

        fun scroll(direction: String): Boolean {
            val service = instance ?: return false
            val root = service.rootInActiveWindow ?: return false
            return try {
                val scrollable = findScrollable(root)
                if (scrollable == null) false
                else {
                    val forward = !direction.lowercase(Locale.US).contains("up") && !direction.lowercase(Locale.US).contains("left")
                    scrollable.performAction(if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                }
            } finally { root.recycle() }
        }

        fun typeText(text: String): Boolean {
            val service = instance ?: return false
            val root = service.rootInActiveWindow ?: return false
            return try {
                val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focused == null) false else {
                    val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
                    focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                }
            } finally { root.recycle() }
        }

        fun findAndClick(textOrDescription: String): Boolean = click(textOrDescription)

        private fun findNode(root: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
            val q = query.trim().lowercase(Locale.US)
            val byText = root.findAccessibilityNodeInfosByText(query)
            byText.firstOrNull { matches(it, q) }?.let { return it }
            return findNodeRecursive(root, q)
        }

        private fun findNodeRecursive(node: AccessibilityNodeInfo, q: String): AccessibilityNodeInfo? {
            if (matches(node, q)) return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = findNodeRecursive(child, q)
                if (found != null) {
                    if (found !== child) child.recycle()
                    return found
                }
                child.recycle()
            }
            return null
        }

        private fun matches(node: AccessibilityNodeInfo, q: String): Boolean {
            val text = node.text?.toString()?.lowercase(Locale.US).orEmpty()
            val desc = node.contentDescription?.toString()?.lowercase(Locale.US).orEmpty()
            return text == q || desc == q || text.contains(q) || desc.contains(q)
        }

        private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
            var current: AccessibilityNodeInfo? = node
            repeat(6) {
                if (current == null) return false
                if (current!!.isClickable) return current!!.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                current = current!!.parent
            }
            return false
        }

        private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isScrollable) return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = findScrollable(child)
                child.recycle()
                if (found != null) return found
            }
            return null
        }
    }
}
