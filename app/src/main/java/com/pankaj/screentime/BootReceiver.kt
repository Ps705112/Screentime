package com.pankaj.screentime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && ScreenTime.hasUsageAccess(context)) {
            try {
                context.startForegroundService(Intent(context, ScreenTimeService::class.java))
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
