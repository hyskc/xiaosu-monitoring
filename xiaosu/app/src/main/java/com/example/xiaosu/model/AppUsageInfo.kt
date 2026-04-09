package com.example.xiaosu.model

import android.graphics.drawable.Drawable

/**
 * 应用使用信息数据模型
 * @param packageName 应用包名
 * @param appName 应用名称
 * @param appIcon 应用图标
 * @param usageTimeInMillis 使用时长（毫秒）
 * @param lastTimeUsed 最后使用时间（毫秒）
 */
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable,
    val usageTimeInMillis: Long,
    val lastTimeUsed: Long
) {
    // 缓存格式化后的使用时长，避免重复计算
    @Transient
    private var _formattedDuration: String? = null
    
    /**
     * 获取格式化后的使用时长
     * 使用懒加载方式，只在首次访问时计算
     */
    fun getFormattedDuration(): String {
        if (_formattedDuration == null) {
            _formattedDuration = formatDuration(usageTimeInMillis)
        }
        return _formattedDuration!!
    }
    
    /**
     * 格式化使用时长
     */
    private fun formatDuration(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
        
        return when {
            hours > 0 -> "${hours}小时${minutes}分钟"
            minutes > 0 -> "${minutes}分钟"
            else -> "不到1分钟"
        }
    }
}