package com.example.xiaosu.ui.about

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.xiaosu.R

class AboutUsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // 设置版本号
        val versionTextView = findViewById<TextView>(R.id.versionTextView)
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            versionTextView.text = "版本 $versionName"
        } catch (e: PackageManager.NameNotFoundException) {
            versionTextView.text = "版本 未知"
        }

        // 设置错误日志按钮点击事件
        val errorLogTextView = findViewById<TextView>(R.id.errorLogTextView)
        errorLogTextView.setOnClickListener {
            // 跳转到错误日志页面
            val intent = Intent(this, ErrorLogActivity::class.java)
            startActivity(intent)
        }
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