package com.example.xiaosu

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.xiaosu.network.FileUploadManager
import com.example.xiaosu.network.NetworkMonitor
import com.example.xiaosu.service.ScreenRecordService
import com.example.xiaosu.ui.fragment.AppUsageFragment
import com.example.xiaosu.ui.fragment.HomeFragment
import com.example.xiaosu.ui.fragment.ProfileFragment
import com.example.xiaosu.ui.fragment.ResourcesFragment
import com.example.xiaosu.ui.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val PERMISSION_REQUEST_CODE = 1001
    private val REQUEST_CODE_AUDIO_PERMISSION_DIALOG = 102
    
    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
    
    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // 如果没有录音权限，显示对话框引导用户前往设置页面
            AlertDialog.Builder(this)
                .setTitle("需要录音权限")
                .setMessage("小米系统限制了录音权限，需要手动开启录音权限才能正常录制声音。是否前往设置页面开启录音权限？")
                .setPositiveButton("去设置") { _, _ ->
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivityForResult(intent, REQUEST_CODE_AUDIO_PERMISSION_DIALOG)
                }
                .setNegativeButton("稍后再说", null)
                .show()
        }
    }

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var networkMonitor: NetworkMonitor

    private val startMediaProjection = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // 检查用户是否已登录
            if (isUserLoggedIn()) {
                startScreenRecordService(result.resultCode, result.data)
                saveMediaProjectionData(result.resultCode, result.data)
            } else {
                Toast.makeText(this, "请先登录后再使用监控功能", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "需要屏幕录制权限才能监控", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 注册网络状态变化广播接收器
        registerNetworkReceiver()

        // 检查并请求权限
        if (!hasPermissions()) {
            requestPermissions()
        } else {
            // 检查录音权限
            checkAudioPermission()
            
            // 检查是否已经有录制服务在运行，如果没有才尝试恢复录制
            if (isUserLoggedIn() && !isScreenRecordServiceRunning()) {
                tryRestoreRecording()
            }
        }
        
        // 检查悬浮窗权限
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
        }
        
        setupBottomNavigation()
        loadFragment(HomeFragment.newInstance()) // 默认加载首页Fragment
    }
    
    // 注册网络状态监听
    private fun registerNetworkReceiver() {
        // 创建 FileUploadManager 实例
        val fileUploadManager = FileUploadManager(this)
        
        // 创建 NetworkMonitor 实例
        networkMonitor = NetworkMonitor(this)
        
        // 设置网络状态回调
        networkMonitor.setNetworkStatusCallback(object : NetworkMonitor.NetworkStatusCallback {
            override fun onWifiConnected() {
                // 当WiFi连接时，通知 FileUploadManager 检查并上传待上传文件
                fileUploadManager.checkNetworkAndUploadPending()
            }
        })
        
        // 注册网络监听
        networkMonitor.register()
        Log.d(TAG, "Network monitor registered")
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment.newInstance())
                    true
                }
                R.id.navigation_resources -> {
                    loadFragment(ResourcesFragment.newInstance())
                    true
                }
                R.id.navigation_app_usage -> {
                    loadFragment(AppUsageFragment.newInstance())
                    true
                }
                R.id.navigation_profile -> {
                    loadFragment(ProfileFragment.newInstance())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun hasPermissions(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // 权限已授予，检查录制服务是否已在运行，如果没有才尝试恢复录制
                if (isUserLoggedIn() && !isScreenRecordServiceRunning()) {
                    tryRestoreRecording()
                }
            } else {
                Toast.makeText(this, "需要所有权限才能正常工作", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun tryRestoreRecording() {
        val prefs = getSharedPreferences("media_projection", Context.MODE_PRIVATE)
        val hasData = prefs.getBoolean("has_data", false)

        if (hasData) {
            // 已有权限数据，直接启动服务
            val resultCode = prefs.getInt("result_code", 0)
            val resultData = Intent()
            // 无法直接保存和恢复Intent，所以这里需要重新请求权限
            requestMediaProjectionPermission()
        } else {
            // 首次运行，请求权限
            requestMediaProjectionPermission()
        }
    }

    private fun requestMediaProjectionPermission() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun startScreenRecordService(resultCode: Int, data: Intent?) {
        if (data == null) return

        val serviceIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordService.EXTRA_RESULT_DATA, data)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "监控已开始", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Screen recording service started")
    }

    private fun saveMediaProjectionData(resultCode: Int, data: Intent?) {
        val prefs = getSharedPreferences("media_projection", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("has_data", true)
            putInt("result_code", resultCode)
            // 无法直接保存Intent，所以只保存标志位
            apply()
        }
    }

    // 检查用户是否已登录
    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_logged_in", false)
    }

    // 检查屏幕录制服务是否正在运行
    private fun isScreenRecordServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ScreenRecordService::class.java.name == service.service.className) {
                Log.d(TAG, "ScreenRecordService is already running")
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        // 检查登录状态，如果用户已退出登录，则停止录制服务
        if (!isUserLoggedIn()) {
            stopScreenRecordService()
        }
    }

    private fun stopScreenRecordService() {
        val serviceIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        startService(serviceIntent)
        Log.d(TAG, "Screen recording service stopped due to logout")
    }

    // 检查是否有悬浮窗权限
    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true // 在Android 6.0以下，不需要动态申请悬浮窗权限
        }
    }
    
    // 请求悬浮窗权限
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            Toast.makeText(this, "请授予悬浮窗权限以显示录屏状态", Toast.LENGTH_LONG).show()
            startActivity(intent)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // 检查悬浮窗权限是否已授予
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Log.d(TAG, "悬浮窗权限已授予")
            } else {
                Log.d(TAG, "悬浮窗权限被拒绝")
                Toast.makeText(this, "需要悬浮窗权限才能显示录屏状态", Toast.LENGTH_SHORT).show()
            }
        }
        
        if (requestCode == REQUEST_CODE_AUDIO_PERMISSION_DIALOG) {
            // 用户从设置页面返回，检查录音权限是否已获取
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "录音权限已获取，可以正常录制声音", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 注销网络状态监听
        try {
            networkMonitor.unregister()
            Log.d(TAG, "Network monitor unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering network monitor: ${e.message}")
        }
        // 应用关闭时不停止录屏服务，让它在后台继续运行
    }
}