package com.example.xiaosu.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.xiaosu.model.AppUsageInfo

class AppUsageHelper(private val context: Context) {

    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private val packageManager: PackageManager by lazy {
        context.packageManager
    }

    fun getAppUsageStats(startTime: Long, endTime: Long): List<AppUsageInfo> {
        val usageStatsMap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
        } else {
            emptyList()
        }

        val appUsageMap = mutableMapOf<String, AppUsageInfoBuilder>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)

                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND,
                    UsageEvents.Event.MOVE_TO_BACKGROUND,
                    UsageEvents.Event.ACTIVITY_RESUMED,
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        val packageName = event.packageName
                        if (packageName != context.packageName) {
                            val builder = appUsageMap.getOrPut(packageName) {
                                AppUsageInfoBuilder(packageName)
                            }
                            builder.lastTimeUsed = event.timeStamp
                        }
                    }
                }
            }
        }

        for (stats in usageStatsMap) {
            if (stats.packageName != context.packageName && stats.totalTimeInForeground > 0) {
                val builder = appUsageMap.getOrPut(stats.packageName) {
                    AppUsageInfoBuilder(stats.packageName)
                }
                builder.usageTimeInMillis = stats.totalTimeInForeground
                if (builder.lastTimeUsed == 0L) {
                    builder.lastTimeUsed = stats.lastTimeUsed
                }
            }
        }

        return appUsageMap.mapNotNull { (packageName, builder) ->
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val appIcon = packageManager.getApplicationIcon(appInfo)

                AppUsageInfo(
                    packageName = packageName,
                    appName = appName,
                    appIcon = appIcon,
                    usageTimeInMillis = builder.usageTimeInMillis,
                    lastTimeUsed = builder.lastTimeUsed
                )
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    private class AppUsageInfoBuilder(val packageName: String) {
        var usageTimeInMillis: Long = 0L
        var lastTimeUsed: Long = 0L
    }
}