package com.example.xiaosuparent.activities

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.xiaosuparent.R
import java.util.concurrent.TimeUnit

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var videoSeekBar: SeekBar
    private lateinit var currentTimeTextView: TextView
    private lateinit var totalTimeTextView: TextView
    private lateinit var videoTitleTextView: TextView
    private lateinit var playPauseButton: ImageButton
    
    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    
    companion object {
        const val EXTRA_VIDEO_URL = "extra_video_url"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        
        // 初始化视图
        initViews()
        
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
        
        // 设置视频源
        setupVideo(videoUrl)
        
        // 设置控件监听器
        setupListeners()
    }
    
    private fun initViews() {
        videoView = findViewById(R.id.videoView)
        videoSeekBar = findViewById(R.id.videoSeekBar)
        currentTimeTextView = findViewById(R.id.currentTimeTextView)
        totalTimeTextView = findViewById(R.id.totalTimeTextView)
        videoTitleTextView = findViewById(R.id.videoTitleTextView)
        playPauseButton = findViewById(R.id.playPauseButton)
    }
    
    private fun setupVideo(videoUrl: String) {
        try {
            // 设置视频源
            videoView.setVideoURI(Uri.parse(videoUrl))
            
            // 准备完成后开始播放
            videoView.setOnPreparedListener { mediaPlayer ->
                // 获取视频总时长
                val duration = mediaPlayer.duration
                videoSeekBar.max = duration
                totalTimeTextView.text = formatTime(duration.toLong())
                
                // 开始播放
                videoView.start()
                isPlaying = true
                updatePlayPauseButton()
                
                // 开始更新进度
                startProgressUpdate()
            }
            
            // 播放完成监听
            videoView.setOnCompletionListener {
                isPlaying = false
                updatePlayPauseButton()
            }
            
            // 错误监听
            videoView.setOnErrorListener { _, _, _ ->
                Toast.makeText(this, "视频播放出错", Toast.LENGTH_SHORT).show()
                true
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "无法播放视频: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupListeners() {
        // 播放/暂停按钮点击事件
        playPauseButton.setOnClickListener {
            togglePlayPause()
        }
        
        // 进度条拖动事件
        videoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // 用户拖动进度条时更新当前时间显示
                    currentTimeTextView.text = formatTime(progress.toLong())
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 开始拖动时暂停更新进度
                handler.removeCallbacksAndMessages(null)
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 停止拖动时跳转到指定位置
                seekBar?.progress?.let { progress ->
                    videoView.seekTo(progress)
                    // 如果视频正在播放，继续更新进度
                    if (isPlaying) {
                        startProgressUpdate()
                    }
                }
            }
        })
    }
    
    private fun togglePlayPause() {
        if (isPlaying) {
            // 暂停播放
            videoView.pause()
            handler.removeCallbacksAndMessages(null)
        } else {
            // 继续播放
            videoView.start()
            startProgressUpdate()
        }
        
        isPlaying = !isPlaying
        updatePlayPauseButton()
    }
    
    private fun updatePlayPauseButton() {
        // 根据播放状态更新按钮图标
        playPauseButton.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause 
            else android.R.drawable.ic_media_play
        )
    }
    
    private fun startProgressUpdate() {
        handler.post(object : Runnable {
            override fun run() {
                try {
                    // 获取当前播放位置
                    val currentPosition = videoView.currentPosition
                    
                    // 更新进度条
                    videoSeekBar.progress = currentPosition
                    
                    // 更新当前时间显示
                    currentTimeTextView.text = formatTime(currentPosition.toLong())
                    
                    // 每100毫秒更新一次
                    handler.postDelayed(this, 100)
                } catch (e: Exception) {
                    // 忽略异常
                }
            }
        })
    }
    
    private fun formatTime(timeMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs) - 
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    override fun onPause() {
        super.onPause()
        // 暂停视频播放
        if (isPlaying) {
            videoView.pause()
        }
        // 停止进度更新
        handler.removeCallbacksAndMessages(null)
    }
    
    override fun onResume() {
        super.onResume()
        // 如果之前是播放状态，恢复播放
        if (isPlaying) {
            videoView.start()
            startProgressUpdate()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 释放资源
        handler.removeCallbacksAndMessages(null)
        videoView.stopPlayback()
    }
}