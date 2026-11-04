package com.firetv.launcher.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import android.util.Log
import com.firetv.launcher.MainActivity
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * High-speed Home button watcher for FireOS 5.
 * Carefully filters for actual Home screen invocations while exempting
 * system Settings and control panels.
 */
class HomeWatcherService : Service() {

    private var job: Job? = null
    private var isRunning = false
    private var lastLaunchTime = 0L

    companion object {
        private const val TAG = "HomeWatcherService"
        private const val DEBOUNCE_MS = 250L

        @Volatile
        var pauseUntilTimestamp = 0L

        /**
         * Temporarily pause interception (e.g. when opening Settings intentionally).
         */
        fun pauseFor(millis: Long) {
            pauseUntilTimestamp = System.currentTimeMillis() + millis
            Log.d(TAG, "HomeWatcher paused for ${millis}ms")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startWatching()
        }
        return START_STICKY
    }

    private fun startWatching() {
        isRunning = true
        Log.d(TAG, "Starting HomeWatcher...")

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
            } catch (_: Exception) {}

            while (isActive && isRunning) {
                var process: java.lang.Process? = null
                var reader: BufferedReader? = null
                try {
                    val cmd = arrayOf("logcat", "-b", "system", "-v", "brief", "ActivityManager:I", "*:S")
                    process = Runtime.getRuntime().exec(cmd)
                    reader = BufferedReader(InputStreamReader(process.inputStream), 1024)

                    while (isActive && isRunning) {
                        val line = reader.readLine() ?: break

                        // Specifically match Home intent or Home screen activities
                        val isHomeIntent = line.contains("cat=[android.intent.category.HOME]") ||
                                line.contains("HomeActivity_vNext") ||
                                line.contains(".ui.HomeActivity") ||
                                line.contains("com.amazon.firehomelauncher")

                        // Explicitly exempt Settings screens and self
                        val isSettings = line.contains("Settings", ignoreCase = true) ||
                                line.contains("com.amazon.tv.settings") ||
                                line.contains("ControlPanel")

                        val isSelf = line.contains("com.firetv.launcher")
                        val isPaused = System.currentTimeMillis() < pauseUntilTimestamp

                        if (isHomeIntent && !isSettings && !isSelf && !isPaused) {
                            val now = System.currentTimeMillis()
                            if (now - lastLaunchTime > DEBOUNCE_MS) {
                                lastLaunchTime = now

                                val homeIntent = Intent(this@HomeWatcherService, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                }
                                startActivity(homeIntent)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Stream error: ${e.message}")
                    delay(1000L)
                } finally {
                    try { reader?.close() } catch (_: Exception) {}
                    try { process?.destroy() } catch (_: Exception) {}
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        job?.cancel()
    }
}
