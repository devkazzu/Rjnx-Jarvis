package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        ScreenContentHolder.setServiceActive(true)
        Log.d("JarvisAccessibility", "Screen Awareness Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: ""
        if (pkgName.isEmpty() || pkgName == packageName) return

        val rootNode = rootInActiveWindow ?: return
        try {
            val textBuilder = StringBuilder()
            traverseNode(rootNode, textBuilder, depth = 0)
            val fullText = textBuilder.toString().trim()
            if (fullText.isNotBlank()) {
                ScreenContentHolder.updateScreenContent(fullText, pkgName)
            }
        } catch (e: Exception) {
            Log.e("JarvisAccessibility", "Error parsing screen content", e)
        } finally {
            rootNode.recycle()
        }
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, builder: StringBuilder, depth: Int) {
        if (node == null || depth > 12) return

        val text = node.text?.toString()?.trim()
        val contentDesc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrBlank()) {
            builder.append(text).append("\n")
        } else if (!contentDesc.isNullOrBlank()) {
            builder.append("[").append(contentDesc).append("]\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNode(child, builder, depth + 1)
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
        Log.w("JarvisAccessibility", "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        ScreenContentHolder.setServiceActive(false)
        Log.d("JarvisAccessibility", "Accessibility service destroyed")
    }
}
