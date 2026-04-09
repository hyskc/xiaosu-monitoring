package com.example.xiaosu.ui.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.xiaosu.R
import com.example.xiaosu.service.ScreenRecordService

class HomeFragment : Fragment() {

    private lateinit var statusTextView: TextView
    private lateinit var infoTextView: TextView
    private lateinit var monitoringIcon: ImageView
    private lateinit var restartButton: Button
    private var recordingStatusReceiver: BroadcastReceiver? = null

    // 注册媒体投影权限请求
    private val startMediaProjection = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // 检查用户是否已登录
            if (isUserLoggedIn()) {
                startScreenRecordService(result.resultCode, result.data)
                saveMediaProjectionData(result.resultCode, result.data)
                updateMonitoringStatus()
            } else {
                Toast.makeText(requireContext(), "请先登录后再使用监控功能", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "需要屏幕录制权限才能监控", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        statusTextView = view.findViewById(R.id.statusTextView)
        infoTextView = view.findViewById(R.id.descriptionTextView)
        monitoringIcon = view.findViewById(R.id.monitoringIcon)
        restartButton = view.findViewById(R.id.restartButton)
        
        restartButton.setOnClickListener {
            requestMediaProjectionPermission()
        }
        
        updateMonitoringStatus()
        
        return view
    }
    
    override fun onResume() {
        super.onResume()
        updateMonitoringStatus()
        // 注册广播接收器来监听录屏状态变化
        registerRecordingStatusReceiver()
    }
    
    override fun onPause() {
        super.onPause()
        // 取消注册广播接收器
        unregisterRecordingStatusReceiver()
    }
    
    private fun registerRecordingStatusReceiver() {
        if (recordingStatusReceiver == null) {
            recordingStatusReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    updateMonitoringStatus()
                }
            }
            requireContext().registerReceiver(
                recordingStatusReceiver,
                IntentFilter("com.example.xiaosu.RECORDING_STATUS_CHANGED"),
                Context.RECEIVER_NOT_EXPORTED
            )
        }
    }
    
    private fun unregisterRecordingStatusReceiver() {
        recordingStatusReceiver?.let {
            try {
                requireContext().unregisterReceiver(it)
            } catch (e: Exception) {
                // 忽略可能的异常
            }
            recordingStatusReceiver = null
        }
    }
    
    private fun updateMonitoringStatus() {
        // 检查用户是否已登录
        val isLoggedIn = isUserLoggedIn()
        val isServiceRunning = isServiceRunning(ScreenRecordService::class.java)
        val isRecordingDisabled = com.example.xiaosu.ui.settings.DisableRecordingActivity.isRecordingDisabled(requireContext())
        
        if (isServiceRunning && isLoggedIn && !isRecordingDisabled) {
            statusTextView.text = getString(R.string.monitoring)
            statusTextView.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            infoTextView.text = getString(R.string.monitoring_info)
            monitoringIcon.setColorFilter(resources.getColor(android.R.color.holo_green_dark, null))
            restartButton.visibility = View.GONE
        } else {
            statusTextView.text = "监控未运行"
            statusTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            
            if (!isLoggedIn) {
                infoTextView.text = "请先登录后再使用监控功能"
                restartButton.visibility = View.GONE
            } else if (isRecordingDisabled) {
                infoTextView.text = "录屏功能已关闭，可在\"我的-应用设置-关闭录屏\"中开启"
                restartButton.visibility = View.GONE
            } else {
                infoTextView.text = "监控服务已停止，点击下方按钮重新启动"
                restartButton.visibility = View.VISIBLE
            }
            
            monitoringIcon.setColorFilter(resources.getColor(android.R.color.holo_red_dark, null))
        }
    }
    
    // 检查用户是否已登录
    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_logged_in", false)
    }
    
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    // 请求媒体投影权限
    private fun requestMediaProjectionPermission() {
        val mediaProjectionManager = requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    // 启动屏幕录制服务
    private fun startScreenRecordService(resultCode: Int, data: Intent?) {
        if (data == null) return

        val serviceIntent = Intent(requireContext(), ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordService.EXTRA_RESULT_DATA, data)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent)
        } else {
            requireContext().startService(serviceIntent)
        }

        Toast.makeText(requireContext(), "监控已重新启动", Toast.LENGTH_SHORT).show()
    }

    // 保存媒体投影数据
    private fun saveMediaProjectionData(resultCode: Int, data: Intent?) {
        val prefs = requireContext().getSharedPreferences("media_projection", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("has_data", true)
            putInt("result_code", resultCode)
            // 无法直接保存Intent，所以只保存标志位
            apply()
        }
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}