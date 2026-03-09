package com.example.xiaosuparent.activities

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.xiaosuparent.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView

class ExoPlayerActivity : AppCompatActivity() {

    private lateinit var playerView: StyledPlayerView
    private lateinit var videoTitleTextView: TextView
    private var player: ExoPlayer? = null
    
    companion object {
        const val EXTRA_VIDEO_URL = "extra_video_url"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exo_player)
        
        // 初始化视图
        playerView = findViewById(R.id.playerView)
        videoTitleTextView = findViewById(R.id.videoTitleTextView)
        
        // 获取传递的视频URL和标题
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "未知视频"
        
        // 设置视频标题
        videoTitleTextView.text = videoTitle
        
        if (videoUrl.isNullOrEmpty()) {
            Toast.makeText(this, "无效的视频链接", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // 初始化播放器
        initializePlayer(videoUrl)
    }
    
    private fun initializePlayer(videoUrl: String) {
        try {
            // 记录视频URL到日志，便于调试
            android.util.Log.d("ExoPlayerActivity", "正在初始化播放器，视频URL: $videoUrl")
            
            // 创建ExoPlayer实例
            player = ExoPlayer.Builder(this).build()
            
            // 将播放器附加到视图
            playerView.player = player
            
            // 验证URL格式
            if (!videoUrl.startsWith("http://") && !videoUrl.startsWith("https://")) {
                throw IllegalArgumentException("无效的URL格式: $videoUrl")
            }
            
            // 创建媒体项
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            
            // 设置媒体项
            player?.setMediaItem(mediaItem)
            
            // 准备播放器
            player?.prepare()
            
            // 自动播放
            player?.playWhenReady = true
            
            // 设置播放器监听器
            setupPlayerListeners()
            
            // 显示加载提示
            Toast.makeText(this, "正在加载视频...", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            // 记录详细错误信息到日志
            android.util.Log.e("ExoPlayerActivity", "初始化播放器失败: ${e.message}", e)
            
            val errorMessage = when (e) {
                is IllegalArgumentException -> "无效的视频URL: ${e.message}"
                else -> "无法初始化播放器: ${e.message}"
            }
            
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupPlayerListeners() {
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        // 缓冲中
                    }
                    Player.STATE_READY -> {
                        // 准备好播放
                    }
                    Player.STATE_ENDED -> {
                        // 播放结束
                    }
                    Player.STATE_IDLE -> {
                        // 空闲状态
                    }
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                // 播放错误
                val errorMessage = when {
                    error.message?.contains("404") == true -> {
                        "视频文件未找到(404错误)，请确认文件是否存在或联系管理员"
                    }
                    error.message?.contains("timeout") == true || error.message?.contains("timed out") == true -> {
                        "连接超时，请检查网络连接"
                    }
                    error.message?.contains("Unable to connect") == true || error.message?.contains("Failed to connect") == true -> {
                        "无法连接到服务器，请检查网络连接或服务器状态"
                    }
                    else -> "播放错误: ${error.message}"
                }
                
                Toast.makeText(this@ExoPlayerActivity, errorMessage, Toast.LENGTH_LONG).show()
                
                // 记录详细错误信息到日志
                android.util.Log.e("ExoPlayerActivity", "播放错误: ${error.message}", error)
            }
        })
    }
    
    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }
    
    private fun hideSystemUI() {
        // 隐藏系统UI，实现沉浸式体验
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
    
    override fun onPause() {
        super.onPause()
        // 暂停播放
        player?.playWhenReady = false
    }
    
    override fun onStop() {
        super.onStop()
        // 释放播放器
        releasePlayer()
    }
    
    private fun releasePlayer() {
        player?.release()
        player = null
    }
}