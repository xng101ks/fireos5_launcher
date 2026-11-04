package com.firetv.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.firetv.launcher.MainActivity

/**
 * An AccessibilityService designed to detect when FireOS's stock home screen
 * (com.amazon.tv.launcher or com.amazon.firehomelauncher) comes to the foreground on Home button press,
 * and redirects immediately back to our custom launcher without transition delay or flickering.
 */
class FireTvAccessibilityService : AccessibilityService() {

    companion object {
        private val AMAZON_LAUNCHER_PACKAGES = setOf(
            "com.amazon.tv.launcher",
            "com.amazon.firehomelauncher"
        )
        private const val LAUNCH_DEBOUNCE_MS = 300L
    }

    private var lastLaunchTimestamp = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        android.util.Log.d("FireTvLauncher", "Accessibility Service Connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (AMAZON_LAUNCHER_PACKAGES.contains(packageName)) {
                val now = System.currentTimeMillis()
                // Prevent duplicate rapid-fire intent triggers
                if (now - lastLaunchTimestamp > LAUNCH_DEBOUNCE_MS) {
                    lastLaunchTimestamp = now
                    redirectToLauncher()
                }
            }
        }
    }

    private fun redirectToLauncher() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Required lifecycle method
    }
}
