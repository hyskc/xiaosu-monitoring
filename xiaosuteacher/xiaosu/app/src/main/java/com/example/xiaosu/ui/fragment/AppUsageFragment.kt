package com.example.xiaosu.ui.fragment

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xiaosu.R
import com.example.xiaosu.adapter.AppUsageAdapter
import com.example.xiaosu.databinding.FragmentAppUsageBinding
import com.example.xiaosu.manager.AppUsageUploadManager
import com.example.xiaosu.model.AppUsageInfo
import com.example.xiaosu.utils.AppUsageHelper
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class AppUsageFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var appUsageAdapter: AppUsageAdapter
    
    // 缓存日期格式化器，避免重复创建
    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_app_usage, container, false)
        
        recyclerView = view.findViewById(R.id.appUsageRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyTextView = view.findViewById(R.id.emptyTextView)
        dateTextView = view.findViewById(R.id.dateTextView)
        
        // 设置当前日期
        dateTextView.text = dateFormat.format(Date())
        
        // 设置RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        appUsageAdapter = AppUsageAdapter()
        recyclerView.adapter = appUsageAdapter
        
        // 设置RecyclerView的缓存策略，减少内存使用
        recyclerView.setItemViewCacheSize(10)
        recyclerView.setHasFixedSize(true)
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 检查是否有使用情况访问权限
        if (hasUsageStatsPermission()) {
            loadAppUsageStats()
            
            // 初始化并安排应用使用情况数据上传任务
            val appUsageUploadManager = AppUsageUploadManager(requireContext())
            appUsageUploadManager.scheduleAppUsageUpload()
        } else {
            requestUsageStatsPermission()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 当用户从设置页面返回时，再次检查权限
        if (hasUsageStatsPermission()) {
            loadAppUsageStats()
        }
    }
    
    // 使用Fragment的lifecycleScope代替自定义coroutineScope，避免内存泄漏

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), requireContext().packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), requireContext().packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        Toast.makeText(requireContext(), "需要访问使用情况的权限来显示应用使用统计", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    private fun loadAppUsageStats() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyTextView.visibility = View.GONE
        
        // 使用lifecycleScope代替自定义coroutineScope
        viewLifecycleOwner.lifecycleScope.launch {
            // 使用Dispatchers.IO进行耗时操作
            val appUsageList = withContext(Dispatchers.IO) {
                getAppUsageStatistics()
            }
            
            // 确保Fragment仍然处于活动状态
            if (!isAdded) return@launch
            
            progressBar.visibility = View.GONE
            
            if (appUsageList.isEmpty()) {
                emptyTextView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyTextView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                // 使用ListAdapter的submitList方法，利用DiffUtil进行高效更新
                appUsageAdapter.submitList(appUsageList)
            }
        }
    }

    private fun getAppUsageStatistics(): List<AppUsageInfo> {
        // 使用AppUsageHelper获取应用使用情况数据
        val appUsageHelper = AppUsageHelper(requireContext())
        val packageManager = requireContext().packageManager
        
        // 获取今天的开始时间（凌晨0点）
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        // 获取当前时间作为结束时间
        val endTime = System.currentTimeMillis()
        
        // 获取应用使用情况数据
        val appUsageList = appUsageHelper.getAppUsageStats(startTime, endTime)
        
        // 为每个应用加载图标
        val appUsageWithIcons = appUsageList.map { appUsage ->
            try {
                val appInfo = packageManager.getApplicationInfo(appUsage.packageName, 0)
                val appIcon = packageManager.getApplicationIcon(appInfo)
                appUsage.copy(appIcon = appIcon)
            } catch (e: PackageManager.NameNotFoundException) {
                appUsage
            }
        }
        
        // 上传应用使用情况数据
        uploadAppUsageData(appUsageList)
        
        // 转换为列表并按使用时间排序（降序）
        // 只显示使用时间超过1秒的应用，减少列表项数量
        return appUsageWithIcons
            .filter { it.usageTime > 1000 } 
            .sortedByDescending { it.usageTime }
    }
    
    /**
     * 上传应用使用情况数据
     * @param appUsageList 应用使用情况列表
     */
    private fun uploadAppUsageData(appUsageList: List<AppUsageInfo>) {
        // 过滤掉使用时间为0的应用
        val filteredAppUsageList = appUsageList.filter { it.usageTime > 0 }
        
        if (filteredAppUsageList.isNotEmpty()) {
            // 使用AppUsageUploadManager上传数据
            val appUsageUploadManager = AppUsageUploadManager(requireContext())
            appUsageUploadManager.uploadAppUsageData(filteredAppUsageList)
        }
    }

    companion object {
        fun newInstance() = AppUsageFragment()
    }
}