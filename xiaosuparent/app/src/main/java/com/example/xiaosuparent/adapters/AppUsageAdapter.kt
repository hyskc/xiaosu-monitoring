package com.example.xiaosuparent.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.xiaosuparent.R
import com.example.xiaosuparent.model.AppUsageInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AppUsageAdapter : RecyclerView.Adapter<AppUsageAdapter.AppUsageViewHolder>() {

    private var appUsageList: MutableList<AppUsageInfo> = mutableListOf()

    // 更新数据
    fun updateData(newData: List<AppUsageInfo>) {
        appUsageList.clear()
        appUsageList.addAll(newData)
        notifyDataSetChanged()
    }

    // 按使用时长排序
    fun sortByUsageDuration(descending: Boolean) {
        appUsageList.sortWith(compareBy { if (descending) -it.usageDuration else it.usageDuration })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppUsageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return AppUsageViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppUsageViewHolder, position: Int) {
        val appUsage = appUsageList[position]
        holder.bind(appUsage)
    }

    override fun getItemCount(): Int = appUsageList.size

    inner class AppUsageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appNameTextView: TextView = itemView.findViewById(R.id.textView_app_name)
        private val usageDurationTextView: TextView = itemView.findViewById(R.id.textView_usage_duration)
        private val launchCountTextView: TextView = itemView.findViewById(R.id.textView_launch_count)
        private val lastUsedTextView: TextView = itemView.findViewById(R.id.textView_last_used)
        private val appIconImageView: ImageView = itemView.findViewById(R.id.imageView_app_icon)
        private val categoryTextView: TextView = itemView.findViewById(R.id.textView_category)

        fun bind(appUsage: AppUsageInfo) {
            appNameTextView.text = appUsage.appName
            usageDurationTextView.text = formatDuration(appUsage.usageDuration)
            launchCountTextView.text = itemView.context.getString(R.string.launch_count, appUsage.launchCount)
            lastUsedTextView.text = itemView.context.getString(R.string.last_used, formatDate(appUsage.lastUsed))
            
            // 设置应用类别（如果有）
            if (!appUsage.category.isNullOrEmpty()) {
                categoryTextView.visibility = View.VISIBLE
                categoryTextView.text = appUsage.category
            } else {
                categoryTextView.visibility = View.GONE
            }
            
            // 加载应用图标（如果有URL）
            if (!appUsage.iconUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(appUsage.iconUrl)
                    .placeholder(R.drawable.ic_app_placeholder)
                    .error(R.drawable.ic_app_placeholder)
                    .into(appIconImageView)
            } else {
                appIconImageView.setImageResource(R.drawable.ic_app_placeholder)
            }
        }

        // 格式化使用时长
        private fun formatDuration(durationMillis: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
            
            return when {
                hours > 0 -> "${hours}小时${minutes}分钟"
                minutes > 0 -> "${minutes}分钟"
                else -> "不到1分钟"
            }
        }

        // 格式化日期
        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}