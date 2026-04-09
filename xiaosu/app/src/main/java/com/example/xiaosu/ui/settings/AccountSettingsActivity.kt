package com.example.xiaosu.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.xiaosu.R
import com.example.xiaosu.service.ScreenRecordService

class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var associatedAccountLayout: LinearLayout
    private lateinit var logoutLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_settings)

        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // 初始化视图
        associatedAccountLayout = findViewById(R.id.associatedAccountLayout)
        logoutLayout = findViewById(R.id.logoutLayout)

        // 设置点击事件
        setupAssociatedAccountListener()
        setupLogoutListener()

        // 设置工具栏返回按钮点击事件
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupAssociatedAccountListener() {
        associatedAccountLayout.setOnClickListener {
            // 检查用户是否已登录
            val sharedPreferences = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
            val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)
            
            if (isLoggedIn) {
                // 跳转到关联账号页面
                val intent = Intent(this, AssociatedAccountActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLogoutListener() {
        logoutLayout.setOnClickListener {
            // 检查用户是否已登录
            val sharedPreferences = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
            val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)
            
            if (isLoggedIn) {
                // 执行退出登录
                logout()
            } else {
                Toast.makeText(this, "您尚未登录", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        // 清除登录状态和用户信息
        val sharedPreferences = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
        
        // 停止屏幕录制服务
        stopScreenRecordService()
        
        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()
        
        // 返回上一页
        finish()
    }
    
    private fun stopScreenRecordService() {
        val serviceIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        startService(serviceIntent)
    }
}