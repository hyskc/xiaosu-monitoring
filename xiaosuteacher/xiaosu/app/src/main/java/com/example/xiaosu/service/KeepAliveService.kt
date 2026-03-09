package com.example.xiaosu.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.xiaosu.MainActivity
import com.example.xiaosu.R
import java.util.Timer
import java.util.TimerTask

/**
 * 保活服务，用于确保ScreenRecordService不被系统杀死
 */
class KeepAliveService : Service() {
    private val TAG = "KeepAliveService"
    private val NOTIFICATION_ID = 2
    private val CHANNEL_ID = "keep_alive_channel"
    private var timer: Timer? = null
    private val CHECK_INTERVAL = 60000L // 每分钟检查一次

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "KeepAliveService created")
        
        // 创建通知渠道
        createNotificationChannel()
        
        // 启动前台服务
        // 在Android 10及以上版本，使用dataSync类型的前台服务
        // 这与AndroidManifest.xml中声明的类型一致
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        
        // 请求忽略电池优化
        requestIgnoreBatteryOptimization()
        
        // 不再使用定时器检查服务，减少电池消耗
        Log.d(TAG, "Service check timer disabled to optimize battery usage")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "KeepAliveService started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "保活服务"
            val descriptionText = "保持应用在后台运行"
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("小苏老师")
            .setContentText("服务正在运行...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    private fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                checkAndRestartRecordingService()
            }
        }, CHECK_INTERVAL, CHECK_INTERVAL)
    }

    private fun checkAndRestartRecordingService() {
        // 检查用户是否已登录
        if (!isUserLoggedIn()) {
            Log.d(TAG, "User not logged in, not starting recording service")
            return
        }

        // 检查录屏是否被禁用
        if (com.example.xiaosu.ui.settings.DisableRecordingActivity.isRecordingDisabled(this)) {
            Log.d(TAG, "Recording is disabled, not starting recording service")
            return
        }

        // 检查ScreenRecordService是否在运行，如果不在则尝试重启
        if (!isServiceRunning(ScreenRecordService::class.java.name)) {
            Log.d(TAG, "ScreenRecordService not running, attempting to restart")
            restartScreenRecordService()
        } else {
            Log.d(TAG, "ScreenRecordService is running")
        }
    }

    private fun isServiceRunning(serviceName: String): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun restartScreenRecordService() {
        // 检查录屏服务是否已在运行
        if (isScreenRecordServiceRunning()) {
            Log.d(TAG, "ScreenRecordService is already running, no need to restart")
            return
        }
        
        // 从SharedPreferences获取保存的MediaProjection数据
        val prefs = getSharedPreferences("media_projection", Context.MODE_PRIVATE)
        val hasData = prefs.getBoolean("has_data", false)

        if (hasData) {
            val resultCode = prefs.getInt("result_code", 0)
            // 由于无法直接恢复Intent，需要通过MainActivity重新获取权限
            // 这里我们可以启动MainActivity并让它处理权限请求
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("REQUEST_MEDIA_PROJECTION", true)
            }
            startActivity(intent)
        } else {
            Log.d(TAG, "No saved media projection data")
        }
    }
    
    // 检查屏幕录制服务是否正在运行
    private fun isScreenRecordServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ScreenRecordService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_logged_in", false)
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    // 这里只记录日志，实际请求应该在MainActivity中进行
                    Log.d(TAG, "App is not ignoring battery optimizations")
                } catch (e: Exception) {
                    Log.e(TAG, "Error requesting ignore battery optimization: ${e.message}")
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "KeepAliveService destroyed, restarting...")
        timer?.cancel()
        timer = null

        // 尝试重启服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, KeepAliveService::class.java))
        } else {
            startService(Intent(this, KeepAliveService::class.java))
        }

        super.onDestroy()
    }
}