package com.pankaj.screentime

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

object ScreenTime {

    fun hasUsageAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            ctx.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Returns (total ms today, per-package ms map). */
    fun computeToday(ctx: Context): Pair<Long, Map<String, Long>> {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val now = System.currentTimeMillis()

        val events = usm.queryEvents(start, now)
        val e = UsageEvents.Event()
        val resumed = HashMap<String, Long>()      // "pkg/class" -> resume time
        val perPkg = HashMap<String, Long>()

        fun add(key: String, end: Long) {
            val s = resumed.remove(key) ?: return
            val pkg = key.substringBefore('/')
            val d = end - s
            if (d > 0) perPkg[pkg] = (perPkg[pkg] ?: 0L) + d
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val key = e.packageName + "/" + (e.className ?: "")
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> resumed[key] = e.timeStamp
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> add(key, e.timeStamp)
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    for (k in resumed.keys.toList()) add(k, e.timeStamp)
                }
            }
        }
        // Apps still on screen right now
        for (k in resumed.keys.toList()) add(k, now)

        return Pair(perPkg.values.sum(), perPkg)
    }

    fun format(ms: Long): String {
        val h = ms / 3_600_000
        val m = (ms / 60_000) % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}
