package com.example.xiaosu.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.xiaosu.R
import com.example.xiaosu.model.AppUsageInfo
import java.text.SimpleDateFormat
import java.util.*

class AppUsageAdapter : ListAdapter<AppUsageInfo, AppUsageAdapter.ViewHolder>(AppUsageDiffCallback()) {
    
    // 缓存SimpleDateFormat实例，避免重复创建
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appUsage = getItem(position)
        holder.bind(appUsage)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIconImageView: ImageView = itemView.findViewById(R.id.appIconImageView)
        private val appNameTextView: TextView = itemView.findViewById(R.id.appNameTextView)
        private val lastTimeUsedTextView: TextView = itemView.findViewById(R.id.lastTimeUsedTextView)
        private val usageDurationTextView: TextView = itemView.findViewById(R.id.usageDurationTextView)

        fun bind(appUsage: AppUsageInfo) {
            // 设置应用图标和名称
            appIconImageView.setImageDrawable(appUsage.appIcon)
            appNameTextView.text = appUsage.appName
            
            // 使用缓存的SimpleDateFormat实例格式化最后使用时间
            val lastUsedTime = timeFormat.format(Date(appUsage.lastTimeUsed))
            lastTimeUsedTextView.text = "最后使用时间: $lastUsedTime"
            
            // 使用AppUsageInfo中的getFormattedDuration方法，避免重复计算
            usageDurationTextView.text = appUsage.getFormattedDuration()
        }
    }
    
    // 使用DiffUtil比较列表项，避免不必要的视图刷新
    class AppUsageDiffCallback : DiffUtil.ItemCallback<AppUsageInfo>() {
        override fun areItemsTheSame(oldItem: AppUsageInfo, newItem: AppUsageInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppUsageInfo, newItem: AppUsageInfo): Boolean {
            return oldItem == newItem
        }
    }
}