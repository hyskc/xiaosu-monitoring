package com.example.xiaosu

import android.app.Application
import com.example.xiaosu.util.FileUtils
import com.example.xiaosu.util.LogUtils

/**
 * 应用程序类
 * 用于初始化全局组件和工具
 */
class XiaosuApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // 初始化日志工具
        LogUtils.init(this)
        
        // 初始化文件工具
        FileUtils.init(this)
    }
}