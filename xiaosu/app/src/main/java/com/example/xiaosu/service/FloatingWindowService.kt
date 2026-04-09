package com.example.xiaosu.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.example.xiaosu.R

class FloatingWindowService : Service() {
    
    companion object {
        const val ACTION_SHOW = "com.example.xiaosu.action.SHOW_FLOATING_WINDOW"
        const val ACTION_HIDE = "com.example.xiaosu.action.HIDE_FLOATING_WINDOW"
        const val ACTION_UPDATE_TEXT = "com.example.xiaosu.action.UPDATE_FLOATING_TEXT"
        const val EXTRA_TEXT = "extra_text"
        private const val TAG = "FloatingWindowService"
    }
    
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var isShowing = false
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingWindowService created")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createFloatingWindow()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_SHOW -> showFloatingWindow()
                ACTION_HIDE -> hideFloatingWindow()
                ACTION_UPDATE_TEXT -> {
                    val text = it.getStringExtra(EXTRA_TEXT) ?: "录制中"
                    updateFloatingText(text)
                }
            }
        }
        return START_STICKY
    }
    
    private fun createFloatingWindow() {
        // 检查是否有悬浮窗权限
        if (!hasOverlayPermission()) {
            Log.e(TAG, "No overlay permission")
            return
        }
        
        // 创建悬浮窗视图
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_window, null)
        
        // 设置悬浮窗参数
        params = WindowManager.LayoutParams().apply {
            // 根据Android版本设置不同的窗口类型
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }
        
        // 设置悬浮窗可拖动
        setupDraggable()
    }
    
    private fun setupDraggable() {
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false
                
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params?.x ?: 0
                        initialY = params?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params?.x = initialX + (event.rawX - initialTouchX).toInt()
                        params?.y = initialY + (event.rawY - initialTouchY).toInt()
                        if (isShowing) {
                            windowManager?.updateViewLayout(floatingView, params)
                        }
                        return true
                    }
                }
                return false
            }
        })
    }
    
    private fun showFloatingWindow() {
        if (floatingView == null || params == null) {
            createFloatingWindow()
        }
        
        if (!isShowing && floatingView != null && params != null) {
            try {
                windowManager?.addView(floatingView, params)
                isShowing = true
                Log.d(TAG, "Floating window shown")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing floating window: ${e.message}")
            }
        }
    }
    
    private fun hideFloatingWindow() {
        if (isShowing && floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
                isShowing = false
                Log.d(TAG, "Floating window hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding floating window: ${e.message}")
            }
        }
    }
    
    private fun updateFloatingText(text: String) {
        floatingView?.findViewById<TextView>(R.id.tvFloatingText)?.text = text
        Log.d(TAG, "Floating text updated to: $text")
    }
    
    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true // 在Android 6.0以下，不需要动态申请悬浮窗权限
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideFloatingWindow()
        Log.d(TAG, "FloatingWindowService destroyed")
    }
}