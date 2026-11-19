package com.pankaj.screentime

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(48), dp(20), dp(20))
        }

        root.addView(TextView(this).apply {
            text = "Screen Time Bar"
            textSize = 26f
            gravity = Gravity.START
        })

        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(status)

        fun btn(label: String, action: () -> Unit) {
            root.addView(Button(this).apply {
                text = label
                setOnClickListener { action() }
            })
        }

        btn("1. Usage Access allow karo") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        btn("2. Notification permission") {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        btn("3. Battery: Unrestricted / Allow") {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
        btn("4. Notification Start karo") {
            startForegroundService(Intent(this, ScreenTimeService::class.java))
            refresh()
        }
        btn("Band karo") {
            stopService(Intent(this, ScreenTimeService::class.java))
        }

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val usage = ScreenTime.hasUsageAccess(this)
        val notif = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        status.text = "Usage Access: " + (if (usage) "OK" else "NAHI") +
            "\nNotification: " + (if (notif) "OK" else "NAHI") +
            (if (usage) "\nAaj ka screen time: " + ScreenTime.format(ScreenTime.computeToday(this).first) else "")
    }
}
