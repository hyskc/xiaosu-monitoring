package com.example.xiaosu.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.xiaosu.R
import com.example.xiaosu.network.ApiClient
import com.example.xiaosu.network.ApiResponse
import com.example.xiaosu.service.ScreenRecordService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.media.projection.MediaProjectionManager

/**
 * 录屏功能管理Activity
 * 可以开启或关闭录屏功能
 * 需要输入家长code验证身份
 */
class DisableRecordingActivity : AppCompatActivity() {

    private lateinit var codeEditText: EditText
    private lateinit var verifyButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusTextView: TextView
    private lateinit var verifyHintTextView: TextView

    companion object {
        // 设置项的键名
        const val PREF_NAME = "app_settings"
        const val KEY_RECORDING_DISABLED = "recording_disabled"

        // 获取录屏是否被禁用的设置
        fun isRecordingDisabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_RECORDING_DISABLED, false)
        }

        // 设置录屏禁用状态
        fun setRecordingDisabled(context: Context, disabled: Boolean) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_RECORDING_DISABLED, disabled).apply()
            
            // 如果禁用录屏，则停止录屏服务
            if (disabled) {
                val intent = Intent(context, ScreenRecordService::class.java)
                intent.action = ScreenRecordService.ACTION_STOP
                context.startService(intent)
                // 发送广播通知UI更新
                context.sendBroadcast(Intent("com.example.xiaosu.RECORDING_STATUS_CHANGED"))
            } else {
                // 如果启用录屏，请求媒体投影权限以启动录屏服务
                val activity = context as? AppCompatActivity
                activity?.let {
                    val mediaProjectionManager = it.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    val intent = mediaProjectionManager.createScreenCaptureIntent()
                    it.startActivityForResult(intent, REQUEST_MEDIA_PROJECTION)
                }
            }
        }
        
        // 媒体投影请求码
        const val REQUEST_MEDIA_PROJECTION = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disable_recording)

        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // 初始化视图
        codeEditText = findViewById(R.id.codeEditText)
        verifyButton = findViewById(R.id.verifyButton)
        progressBar = findViewById(R.id.progressBar)
        statusTextView = findViewById(R.id.statusTextView)
        verifyHintTextView = findViewById(R.id.verifyHintTextView)

        // 设置工具栏返回按钮点击事件
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // 设置验证按钮点击事件
        verifyButton.setOnClickListener {
            verifyParentCode()
        }

        // 显示当前状态
        updateStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                // 用户同意录屏，启动录屏服务
                startScreenRecordService(resultCode, data)
                // 发送广播通知UI更新
                sendBroadcast(Intent("com.example.xiaosu.RECORDING_STATUS_CHANGED"))
            } else {
                // 用户拒绝录屏，保持禁用状态
                setRecordingDisabled(this, true)
                // 发送广播通知UI更新
                sendBroadcast(Intent("com.example.xiaosu.RECORDING_STATUS_CHANGED"))
            }
            updateStatus() // 更新UI状态
        }
    }
    
    private fun startScreenRecordService(resultCode: Int, data: Intent) {
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
    }

    private fun updateStatus() {
        val isDisabled = isRecordingDisabled(this)
        if (isDisabled) {
            statusTextView.text = "当前状态：录屏功能已关闭"
            statusTextView.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            verifyButton.text = "验证并开启录屏"
            verifyHintTextView.text = "请输入家长验证码以开启录屏功能"
        } else {
            statusTextView.text = "当前状态：录屏功能已开启"
            statusTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            verifyButton.text = "验证并关闭录屏"
            verifyHintTextView.text = "请输入家长验证码以关闭录屏功能"
        }
    }

    private fun verifyParentCode() {
        val code = codeEditText.text.toString().trim()
        
        if (code.isEmpty()) {
            Toast.makeText(this, "请输入家长验证码", Toast.LENGTH_SHORT).show()
            return
        }

        // 显示进度条
        progressBar.visibility = View.VISIBLE
        verifyButton.isEnabled = false

        // 调用API验证家长code
        val apiService = ApiClient.apiService
        val call = apiService.validateParentCode(code)

        call.enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(
                call: Call<ApiResponse<Map<String, Any>>>,
                response: Response<ApiResponse<Map<String, Any>>>
            ) {
                progressBar.visibility = View.GONE
                verifyButton.isEnabled = true

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.success) {
                        // 验证成功，根据当前状态切换录屏功能
                        val currentStatus = isRecordingDisabled(this@DisableRecordingActivity)
                        val newStatus = !currentStatus
                        setRecordingDisabled(this@DisableRecordingActivity, newStatus)
                        
                        val message = if (newStatus) {
                            "验证成功，已关闭录屏功能"
                        } else {
                            "验证成功，已开启录屏功能"
                        }
                        
                        Toast.makeText(this@DisableRecordingActivity, message, Toast.LENGTH_SHORT).show()
                        updateStatus()
                    } else {
                        // 验证失败
                        Toast.makeText(
                            this@DisableRecordingActivity,
                            apiResponse?.message ?: "验证失败，请检查验证码",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // 请求失败
                    Toast.makeText(this@DisableRecordingActivity, "请求失败，请稍后重试", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                progressBar.visibility = View.GONE
                verifyButton.isEnabled = true
                Toast.makeText(this@DisableRecordingActivity, "网络错误：${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}