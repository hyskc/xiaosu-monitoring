package com.example.xiaosuparent.model

data class AppUsageInfo(
    val packageName: String,        // 应用包名
    val appName: String,           // 应用名称
    val usageDuration: Long,       // 使用时长（毫秒）
    val launchCount: Int,          // 启动次数
    val lastUsed: Long,            // 最后使用时间（时间戳）
    val iconUrl: String?,          // 应用图标URL（可选）
    val category: String?          // 应用类别（可选，如：社交、游戏、学习等）
)