package com.example.xiaosu.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.xiaosu.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var screenOffStopRecordingSwitch: SwitchCompat
    private lateinit var wifiOnlyUploadSwitch: SwitchCompat
    private lateinit var autoStartSwitch: SwitchCompat
    private lateinit var audioPermissionButton: Button
    
    private val AUTOSTART_PERMISSION_REQUEST_CODE = 1002
    private val AUDIO_PERMISSION_REQUEST_CODE = 1003

    companion object {
        // 设置项的键名
        const val PREF_NAME = "app_settings"
        const val KEY_SCREEN_OFF_STOP_RECORDING = "screen_off_stop_recording"
        const val KEY_WIFI_ONLY_UPLOAD = "wifi_only_upload"
        const val KEY_AUTO_START = "auto_start"

        // 获取息屏时不录屏的设置
        fun isScreenOffStopRecordingEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_SCREEN_OFF_STOP_RECORDING, true)
        }

        // 获取使用数据网络上传的设置
        fun isWifiOnlyUploadEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_WIFI_ONLY_UPLOAD, false)
        }
        
        // 获取自启动设置
        fun isAutoStartEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_AUTO_START, false) // 默认为关闭状态
        }

        // 检查当前是否连接到WIFI
        fun isConnectedToWifi(context: Context): Boolean {
            val networkMonitor = com.example.xiaosu.network.NetworkMonitor(context)
            return networkMonitor.isConnectedToWifi()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // 初始化开关控件
        screenOffStopRecordingSwitch = findViewById(R.id.screenOffStopRecordingSwitch)
        wifiOnlyUploadSwitch = findViewById(R.id.wifiOnlyUploadSwitch)
        autoStartSwitch = findViewById(R.id.autoStartSwitch)
        audioPermissionButton = findViewById(R.id.audioPermissionButton)

        // 加载保存的设置
        loadSettings()

        // 设置开关监听器
        setupSwitchListeners()
        
        // 设置关闭录屏功能按钮点击事件
        findViewById<Button>(R.id.disableRecordingButton).setOnClickListener {
            startActivity(Intent(this, DisableRecordingActivity::class.java))
        }

        // 设置工具栏返回按钮点击事件
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        screenOffStopRecordingSwitch.isChecked = prefs.getBoolean(KEY_SCREEN_OFF_STOP_RECORDING, false)
        wifiOnlyUploadSwitch.isChecked = prefs.getBoolean(KEY_WIFI_ONLY_UPLOAD, false)
        autoStartSwitch.isChecked = prefs.getBoolean(KEY_AUTO_START, false)
        
        // 检查录音权限状态
        updateAudioPermissionButtonState()
    }

    private fun setupSwitchListeners() {
        // 录音权限按钮
        updateAudioPermissionButtonState()
        audioPermissionButton.setOnClickListener {
            requestAudioPermission()
        }
        
        // 息屏时进行录屏开关
        screenOffStopRecordingSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveScreenOffStopRecordingSetting(isChecked)
            if (isChecked) {
                Toast.makeText(this, "已开启息屏时进行录屏功能", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "已关闭息屏时进行录屏功能", Toast.LENGTH_SHORT).show()
            }
        }

        // 使用数据网络上传开关
        wifiOnlyUploadSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveWifiOnlyUploadSetting(isChecked)
            if (isChecked) {
                Toast.makeText(this, "已开启使用数据网络上传功能", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "已关闭使用数据网络上传功能", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 自启动开关
        autoStartSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // 请求自启动权限
                requestAutoStartPermission()
            } else {
                // 直接保存设置
                saveAutoStartSetting(false)
                Toast.makeText(this, "已关闭开机自启动功能", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveScreenOffStopRecordingSetting(enabled: Boolean) {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SCREEN_OFF_STOP_RECORDING, enabled).apply()
    }

    private fun saveWifiOnlyUploadSetting(enabled: Boolean) {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_WIFI_ONLY_UPLOAD, enabled).apply()
    }
    
    private fun saveAutoStartSetting(enabled: Boolean) {
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }
    
    private fun requestAutoStartPermission() {
        // 请求自启动权限，不同厂商的手机有不同的权限设置页面
        // 这里我们使用通用的方式，跳转到应用详情页面
        try {
            val intent = Intent()
            intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = android.net.Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivityForResult(intent, AUTOSTART_PERMISSION_REQUEST_CODE)
            Toast.makeText(this, "请在权限管理中开启自启动权限", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开权限设置页面，请手动开启自启动权限", Toast.LENGTH_LONG).show()
            // 如果无法打开权限页面，则不启用自启动
            autoStartSwitch.isChecked = false
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOSTART_PERMISSION_REQUEST_CODE) {
            // 用户从权限设置页面返回，假设用户已授予权限
            // 实际上我们无法确定用户是否真的授予了权限，因为不同厂商的权限管理机制不同
            // 所以这里我们只能假设用户已经授予了权限
            saveAutoStartSetting(true)
            Toast.makeText(this, "已开启开机自启动功能", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateAudioPermissionButtonState() {
        if (hasAudioPermission()) {
            audioPermissionButton.text = "已获取权限"
            audioPermissionButton.isEnabled = false
        } else {
            audioPermissionButton.text = "获取权限"
            audioPermissionButton.isEnabled = true
        }
    }
    
    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestAudioPermission() {
        if (!hasAudioPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
                Toast.makeText(this, "录音权限已获取", Toast.LENGTH_SHORT).show()
                updateAudioPermissionButtonState()
            } else {
                // 权限被拒绝
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                    // 用户选择了"不再询问"选项，引导用户手动开启权限
                    AlertDialog.Builder(this)
                        .setTitle("需要录音权限")
                        .setMessage("录屏功能需要录音权限才能正常工作。请在设置中手动开启录音权限。")
                        .setPositiveButton("去设置") { _, _ ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        .setNegativeButton("取消", null)
                        .show()
                } else {
                    Toast.makeText(this, "录音权限被拒绝，录屏时将无法录制声音", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}