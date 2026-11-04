package com.firetv.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.firetv.launcher.MainActivity

/**
 * Ensures that the custom launcher is immediately started whenever
 * the Fire TV finishes booting.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // Start the background home button watcher
            val serviceIntent = Intent(context, com.firetv.launcher.service.HomeWatcherService::class.java)
            context.startService(serviceIntent)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(launchIntent)
        }
    }
}
