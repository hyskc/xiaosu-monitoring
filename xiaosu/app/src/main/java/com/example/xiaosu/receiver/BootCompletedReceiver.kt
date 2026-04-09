package com.example.xiaosu.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.xiaosu.MainActivity

class BootCompletedReceiver : BroadcastReceiver() {
    private val TAG = "BootCompletedReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, checking auto start setting")
            
            // 检查是否开启了自启动功能
            val isAutoStartEnabled = com.example.xiaosu.ui.settings.SettingsActivity.isAutoStartEnabled(context)
            
            if (isAutoStartEnabled) {
                Log.d(TAG, "Auto start is enabled, starting app")
                // 启动应用的MainActivity
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(launchIntent)
            } else {
                Log.d(TAG, "Auto start is disabled, not starting app")
            }
        }
    }
}