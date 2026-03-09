package com.example.xiaosu.ui.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.xiaosu.R
import com.example.xiaosu.util.LogUtils

class ErrorLogActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var emptyLogTextView: TextView
    private lateinit var copyButton: Button
    private lateinit var clearButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error_log)

        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // 初始化视图
        logTextView = findViewById(R.id.logTextView)
        emptyLogTextView = findViewById(R.id.emptyLogTextView)
        copyButton = findViewById(R.id.copyButton)
        clearButton = findViewById(R.id.clearButton)

        // 加载日志内容
        loadLogs()

        // 设置按钮点击事件
        copyButton.setOnClickListener {
            copyLogsToClipboard()
        }

        clearButton.setOnClickListener {
            clearLogs()
        }
    }

    private fun loadLogs() {
        // 获取所有日志内容
        val logs = LogUtils.getAllLogs()

        if (logs.isNotEmpty()) {
            // 有日志内容，显示日志
            logTextView.text = logs
            logTextView.visibility = View.VISIBLE
            emptyLogTextView.visibility = View.GONE
            copyButton.isEnabled = true
            clearButton.isEnabled = true
        } else {
            // 没有日志内容，显示空日志提示
            logTextView.visibility = View.GONE
            emptyLogTextView.visibility = View.VISIBLE
            copyButton.isEnabled = false
            clearButton.isEnabled = false
        }
    }

    private fun copyLogsToClipboard() {
        val logs = logTextView.text.toString()
        if (logs.isNotEmpty()) {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("错误日志", logs)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(this, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearLogs() {
        // 清除所有日志
        LogUtils.clearLogs()
        // 重新加载日志（此时应该为空）
        loadLogs()
        Toast.makeText(this, "日志已清除", Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 处理返回按钮点击事件
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}