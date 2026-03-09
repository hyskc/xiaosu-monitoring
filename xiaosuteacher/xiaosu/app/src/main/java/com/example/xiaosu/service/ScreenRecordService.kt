package com.example.xiaosu.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.xiaosu.R
import com.example.xiaosu.network.FileUploadManager
import com.example.xiaosu.ui.settings.DisableRecordingActivity
import com.example.xiaosu.util.FileUtils
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicInteger

class ScreenRecordService : Service() {
    companion object {
        const val ACTION_START = "com.example.xiaosu.action.START_RECORDING"
        const val ACTION_STOP = "com.example.xiaosu.action.STOP_RECORDING"
        const val ACTION_PAUSE = "com.example.xiaosu.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "com.example.xiaosu.action.RESUME_RECORDING"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
    }
    
    private val TAG = "ScreenRecordService"
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "screen_record_channel"

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var resultCode: Int = 0
    private var resultData: Intent? = null
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var screenDensity: Int = 0
    private var currentVideoPath: String = ""
    private var uploadManager: FileUploadManager? = null
    private var timer: Timer? = null
    private var recordingDuration = 0L // 录制时长，单位毫秒
    private val MAX_RECORDING_DURATION = 24 * 60 * 60 * 1000L // 24小时，单位毫秒
    private var screenOffReceiver: BroadcastReceiver? = null
    private var wasRecordingBeforeScreenOff = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var restartAttempts = 0
    private val MAX_RESTART_ATTEMPTS = 5
    private var errorHandler: Timer? = null
    // 添加准备状态超时检测相关变量
    private var preparingTimer: Timer? = null
    private val MAX_PREPARING_DURATION = 60 * 1000L // 1分钟，单位毫秒
    private var isPreparing = false

    // 上传状态广播接收器
    private var uploadStatusReceiver: BroadcastReceiver? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        uploadManager = FileUploadManager(this)
        getScreenSize()
        createNotificationChannel()
        registerScreenOffReceiver()
        registerUploadStatusReceiver()
        acquireWakeLock()
        startKeepAliveService()
    }
    
    // 注册上传状态广播接收器
    private fun registerUploadStatusReceiver() {
        uploadStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.xiaosu.UPLOAD_STATUS_CHANGED") {
                    val fileName = intent.getStringExtra("file_name") ?: ""
                    val success = intent.getBooleanExtra("success", false)
                    
                    if (success) {
                        // 上传成功
                        Log.d(TAG, "Upload completed successfully for $fileName")
                        
                        // 如果当前正在录制，更新悬浮窗显示录制中
                        if (isRecording) {
                            updateFloatingWindowText("录制中")
                        } else {
                            // 如果当前没有录制，显示上传成功
                            updateFloatingWindowText("上传成功")
                        }
                    } else {
                        // 上传失败
                        Log.d(TAG, "Upload failed for $fileName")
                        
                        // 如果当前正在录制，更新悬浮窗显示录制中
                        if (isRecording) {
                            updateFloatingWindowText("录制中 (上传失败)")
                            // 3秒后恢复显示录制中
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (isRecording) {
                                    updateFloatingWindowText("录制中")
                                }
                            }, 3000)
                        } else {
                            // 如果当前没有录制，显示上传失败
                            updateFloatingWindowText("上传失败")
                        }
                    }
                }
            }
        }
        
        registerReceiver(uploadStatusReceiver, IntentFilter("com.example.xiaosu.UPLOAD_STATUS_CHANGED"), Context.RECEIVER_NOT_EXPORTED)
        Log.d(TAG, "Upload status receiver registered")
    }

    fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "${packageName}:ScreenRecordWakeLock"
            )
            wakeLock?.acquire(10*60*1000L /*10 minutes*/)
            Log.d(TAG, "WakeLock acquired")
        }
    }

    fun releaseWakeLock() {
        if (wakeLock != null && wakeLock?.isHeld == true) {
            wakeLock?.release()
            wakeLock = null
            Log.d(TAG, "WakeLock released")
        }
    }

    fun startKeepAliveService() {
        val intent = Intent(this, KeepAliveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Log.d(TAG, "KeepAliveService started")
    }

    fun registerScreenOffReceiver() {
        screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        if (!isScreenOffStopRecordingEnabled() && isRecording) {
                            Log.d(TAG, "Screen turned off, pausing recording")
                            wasRecordingBeforeScreenOff = true
                            stopRecording(false, false) // 暂停录制但不上传
                        }
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        if (!isScreenOffStopRecordingEnabled() && wasRecordingBeforeScreenOff) {
                            Log.d(TAG, "Screen turned on, resuming recording")
                            wasRecordingBeforeScreenOff = false
                            startRecording()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    fun isScreenOffStopRecordingEnabled(): Boolean {
        return com.example.xiaosu.ui.settings.SettingsActivity.isScreenOffStopRecordingEnabled(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_STICKY
        }

        // 检查录屏是否被禁用
        if (com.example.xiaosu.ui.settings.DisableRecordingActivity.isRecordingDisabled(this)) {
            Log.d(TAG, "Recording is disabled, not starting service")
            // 发送广播通知UI更新
            sendBroadcast(Intent("com.example.xiaosu.RECORDING_STATUS_CHANGED"))
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_START -> {
                resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA)
                
                // 获取屏幕尺寸和密度
                val displayMetrics = resources.displayMetrics
                screenWidth = displayMetrics.widthPixels
                screenHeight = displayMetrics.heightPixels
                screenDensity = displayMetrics.densityDpi
                
                // 初始化上传管理器
                uploadManager = FileUploadManager(this)
                
                // 注册屏幕关闭广播接收器
                registerScreenOffReceiver()
                
                // 显示悬浮窗
                showFloatingWindow()
                
                // 开始录制
                startRecording()
            }
            ACTION_STOP -> {
                if (isRecording) {
                    stopRecording()
                }
                // 隐藏悬浮窗
                hideFloatingWindow()
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    // 检查用户是否已登录
    fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("is_logged_in", false)
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun getScreenSize() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "屏幕录制"
            val descriptionText = "正在录制屏幕"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("小苏老师")
            .setContentText("正在监控中...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        return builder.build()
    }

    fun startForegroundService() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    fun startRecording() {
        if (resultCode == 0 || resultData == null) {
            Log.e(TAG, "No media projection data")
            return
        }

        // 检查录屏是否被禁用
        if (com.example.xiaosu.ui.settings.DisableRecordingActivity.isRecordingDisabled(this)) {
            Log.d(TAG, "Recording is disabled, not starting recording")
            // 通知UI更新状态
            sendBroadcast(Intent("com.example.xiaosu.RECORDING_STATUS_CHANGED"))
            return
        }
        
        // 显示悬浮窗
        showFloatingWindow()
        
        // 更新悬浮窗文本为"准备录制..."
        updateFloatingWindowText("准备录制...")
        
        // 设置准备状态并启动准备状态超时检测
        isPreparing = true
        startPreparingTimer()

        startForegroundService()

        // 初始化MediaRecorder
        if (!initMediaRecorder()) {
            // 如果初始化失败，尝试重新初始化
            Log.e(TAG, "Failed to initialize MediaRecorder, retrying...")
            scheduleRetry()
            return
        }

        // 初始化MediaProjection，只在mediaProjection为null时创建新实例
        try {
            if (mediaProjection == null) {
                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = projectionManager.getMediaProjection(resultCode, resultData!!)
            }

            // 创建VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenRecording",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            mediaRecorder?.start()
            isRecording = true
            recordingDuration = 0L
            startTimer()
            Log.d(TAG, "Recording started")
            // 更新悬浮窗文本为"录制中"
            updateFloatingWindowText("录制中")
            // 取消准备状态
            isPreparing = false
            cancelPreparingTimer()
            // 发送广播通知UI更新
            sendBroadcast(Intent("com.example.xiaosu.RECORDING_STATUS_CHANGED"))
            // 重置重试计数
            restartAttempts = 0
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording: ${e.message}")
            isRecording = false
            isPreparing = false
            cancelPreparingTimer()
            releaseMediaRecorder()
            releaseVirtualDisplay()
            if (mediaProjection != null && restartAttempts < MAX_RESTART_ATTEMPTS) {
                // 尝试重新启动录制
                scheduleRetry()
            } else {
                mediaProjection?.stop()
                mediaProjection = null
            }
            // 通知UI更新状态
            sendBroadcast(Intent("com.example.xiaosu.RECORDING_STATUS_CHANGED"))
        }
    }

    fun scheduleRetry() {
        restartAttempts++
        if (restartAttempts <= MAX_RESTART_ATTEMPTS) {
            Log.d(TAG, "Scheduling retry attempt $restartAttempts of $MAX_RESTART_ATTEMPTS")
            // 延迟3秒后重试
            errorHandler = Timer()
            errorHandler?.schedule(object : TimerTask() {
                override fun run() {
                    startRecording()
                }
            }, 3000)
        } else {
            Log.e(TAG, "Max retry attempts reached, giving up")
            // 通知UI更新状态
            sendBroadcast(Intent("com.example.xiaosu.RECORDING_STATUS_CHANGED"))
        }
    }

    private var nextVideoFile: File? = null
    private var isUploading = false
    private val MAX_FILE_SIZE = 200 * 1024 * 1024L // 200MB

    fun initMediaRecorder(): Boolean {
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }

            val videoFile = createVideoFile()
            currentVideoPath = videoFile.absolutePath

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(currentVideoPath)
                setVideoSize(screenWidth, screenHeight)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncodingBitRate(5 * 1024 * 1024) // 5 Mbps
                setVideoFrameRate(30)
                
                // 设置最大文件大小
                setMaxFileSize(MAX_FILE_SIZE)
                
                // 设置错误监听器
                setOnErrorListener { mr, what, extra ->
                    Log.e(TAG, "MediaRecorder error: $what, extra: $extra")
                    // 尝试重新启动录制
                    if (isRecording) {
                        stopRecording(true, false)
                        scheduleRetry()
                    }
                }
                
                // 设置信息监听器
                setOnInfoListener { mr, what, extra ->
                    Log.d(TAG, "MediaRecorder info: $what, extra: $extra")
                    when (what) {
                        MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING -> {
                            Log.d(TAG, "Maximum file size approaching, preparing next file")
                            prepareNextOutputFile()
                        }
                        MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED -> {
                            Log.d(TAG, "Maximum file size reached")
                        }
                        MediaRecorder.MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED -> {
                            Log.d(TAG, "Next output file started")
                            // 上传前一个文件
                            uploadPreviousFile()
                        }
                    }
                }
                
                prepare()
            }
            return true
        } catch (e: IOException) {
            Log.e(TAG, "Error initializing MediaRecorder: ${e.message}")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error initializing MediaRecorder: ${e.message}")
            return false
        }
    }

    private fun prepareNextOutputFile() {
        try {
            // 创建下一个视频文件
            nextVideoFile = createVideoFile()
            Log.d(TAG, "Next output file prepared: ${nextVideoFile?.absolutePath}")
            
            // 设置下一个输出文件
            mediaRecorder?.setNextOutputFile(nextVideoFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing next output file: ${e.message}")
        }
    }

    private fun uploadPreviousFile() {
        val previousFile = File(currentVideoPath)
        if (previousFile.exists() && previousFile.length() > 0) {
            // 更新当前视频路径为下一个文件的路径
            currentVideoPath = nextVideoFile?.absolutePath ?: ""
            nextVideoFile = null
            
            // 在后台线程中上传文件
            Thread {
                Log.d(TAG, "Uploading previous file: ${previousFile.absolutePath}")
                isUploading = true
                updateFloatingWindowText("录制中 (上传中...)")
                uploadManager?.uploadFile(previousFile)
                // 注意：文件上传完成后，FileUploadManager会自动删除本地文件
                isUploading = false
                updateFloatingWindowText("录制中")
            }.start()
        }
    }

    fun createVideoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        
        // 使用FileUtils在手机根目录下的xiaosu/videos文件夹中创建视频文件
        val videoDir = FileUtils.createDirectory("videos")
        return if (videoDir != null) {
            // 如果成功创建了目录，则在该目录下创建视频文件
            File(videoDir, "SCREEN_$timeStamp.mp4")
        } else {
            // 如果创建目录失败，则使用应用私有目录
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            File(storageDir, "SCREEN_$timeStamp.mp4")
        }
    }

    fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                recordingDuration += 1000 // 每秒增加1000毫秒
                if (recordingDuration >= MAX_RECORDING_DURATION) {
                    // 达到最大录制时长，停止当前录制
                    // 上传完成后，uploadStatusReceiver会负责重启录制
                    stopAndRestartRecording()
                }
            }
        }, 1000, 1000) // 每秒检查一次
    }

    fun stopAndRestartRecording() {
        // 保存当前的resultCode和resultData
        val savedResultCode = resultCode
        val savedResultData = resultData
        
        // 重置录制时长计数器
        recordingDuration = 0L
        
        // 在重启录制前，确保释放mediaProjection
        stopRecording(false) // 不自动重启，我们会手动重启
        
        // 确保mediaProjection被释放，以便下次录制时重新创建
        mediaProjection?.stop()
        mediaProjection = null
        
        // 保存resultCode和resultData以便后续使用
        resultCode = savedResultCode
        resultData = savedResultData
        
        // 更新悬浮窗文本，表明当前状态
        updateFloatingWindowText("准备新录制...")
        Log.d(TAG, "Recording stopped due to max duration, starting new recording immediately")
        
        // 立即开始新的录制，不等待上传完成
        Handler(Looper.getMainLooper()).postDelayed({
            startRecording()
        }, 1000) // 延迟1秒，给系统一些时间释放资源
    }

    fun stopRecording(restart: Boolean = false, upload: Boolean = true) {
        if (timer != null) {
            timer?.cancel()
            timer = null
        }

        if (errorHandler != null) {
            errorHandler?.cancel()
            errorHandler = null
        }

        if (isRecording) {
            // 更新悬浮窗文本为"停止录制..."
            updateFloatingWindowText("停止录制...")
            
            try {
                mediaRecorder?.stop()
                mediaRecorder?.reset()
                Log.d(TAG, "Recording stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording: ${e.message}")
                // 更新悬浮窗文本为"录制出错"
                updateFloatingWindowText("录制出错")
                // 即使停止失败，也继续清理资源
            }
            
            releaseMediaRecorder()
            releaseVirtualDisplay()
            
            isRecording = false
            
            // 发送广播通知UI更新
            sendBroadcast(Intent("com.example.xiaosu.RECORDING_STATUS_CHANGED"))
            
            // 处理当前视频文件和下一个视频文件（如果存在）
            val filesToUpload = mutableListOf<File>()
            
            // 添加当前视频文件（如果存在且有效）
            if (currentVideoPath.isNotEmpty()) {
                val currentFile = File(currentVideoPath)
                if (currentFile.exists() && currentFile.length() > 0) {
                    filesToUpload.add(currentFile)
                } else {
                    Log.e(TAG, "Current video file does not exist or is empty: $currentVideoPath")
                }
            }
            
            // 添加下一个视频文件（如果存在且有效）
            nextVideoFile?.let { nextFile ->
                if (nextFile.exists() && nextFile.length() > 0) {
                    filesToUpload.add(nextFile)
                    Log.d(TAG, "Added next video file to upload queue: ${nextFile.absolutePath}")
                } else {
                    Log.e(TAG, "Next video file does not exist or is empty: ${nextFile.absolutePath}")
                }
                nextVideoFile = null
            }
            
            // 如果需要上传且有文件需要上传
            if (upload && filesToUpload.isNotEmpty()) {
                // 更新悬浮窗文本为"正在上传..."
                updateFloatingWindowText("正在上传...")
                
                // 创建一个计数器来跟踪上传完成的文件数量
                val uploadCounter = AtomicInteger(filesToUpload.size)
                
                // 上传所有文件
                for (file in filesToUpload) {
                    Thread {
                        Log.d(TAG, "Uploading file: ${file.absolutePath}")
                        // 调用uploadFile方法，上传完成后FileUploadManager会自动删除本地文件
                        uploadManager?.uploadFile(file)
                        
                        // 如果所有文件都已上传完成且需要重启录制
                        if (uploadCounter.decrementAndGet() == 0 && restart) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                startRecording()
                            }, 3000)
                        }
                    }.start()
                }
            } else if (filesToUpload.isEmpty() && restart) {
                // 如果没有文件需要上传但需要重启录制
                updateFloatingWindowText("录制失败")
                Handler(Looper.getMainLooper()).postDelayed({
                    startRecording()
                }, 3000)
            } else if (restart) {
                // 如果不需要上传但需要重启录制
                Handler(Looper.getMainLooper()).postDelayed({
                    startRecording()
                }, 3000)
            }
        }
    }
    
    fun releaseMediaRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }
    
    fun releaseVirtualDisplay() {
        virtualDisplay?.release()
        virtualDisplay = null
    }
    
    // 显示悬浮窗
    fun showFloatingWindow() {
        val intent = Intent(this, FloatingWindowService::class.java)
        intent.action = FloatingWindowService.ACTION_SHOW
        startService(intent)
    }
    
    // 隐藏悬浮窗
    fun hideFloatingWindow() {
        val intent = Intent(this, FloatingWindowService::class.java)
        intent.action = FloatingWindowService.ACTION_HIDE
        startService(intent)
    }
    
    // 更新悬浮窗文本
    fun updateFloatingWindowText(text: String) {
        val intent = Intent(this, FloatingWindowService::class.java)
        intent.action = FloatingWindowService.ACTION_UPDATE_TEXT
        intent.putExtra(FloatingWindowService.EXTRA_TEXT, text)
        startService(intent)
    }
    
    // 添加准备状态超时检测定时器
    fun startPreparingTimer() {
        cancelPreparingTimer() // 先取消之前的定时器
        
        preparingTimer = Timer()
        preparingTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (isPreparing) {
                    Log.d(TAG, "Preparing state timeout check")
                    // 在主线程中执行UI更新和重启录制
                    Handler(Looper.getMainLooper()).post {
                        if (isPreparing) {
                            Log.d(TAG, "Preparing state timeout, restarting recording")
                            // 如果还在准备状态，则重启录制
                            updateFloatingWindowText("准备超时，正在重启...")
                            
                            // 释放资源
                            releaseMediaRecorder()
                            releaseVirtualDisplay()
                            
                            // 重置mediaProjection
                            mediaProjection?.stop()
                            mediaProjection = null
                            
                            // 延迟2秒后重新开始录制
                            Handler(Looper.getMainLooper()).postDelayed({
                                startRecording()
                            }, 2000)
                        }
                    }
                }
            }
        }, MAX_PREPARING_DURATION, MAX_PREPARING_DURATION) // 每分钟检查一次
    }
    
    fun cancelPreparingTimer() {
        preparingTimer?.cancel()
        preparingTimer = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        // 停止录制
        if (isRecording) {
            stopRecording()
        }
        
        // 取消准备状态定时器
        cancelPreparingTimer()
        
        // 释放资源
        releaseMediaRecorder()
        releaseVirtualDisplay()
        
        // 释放mediaProjection
        mediaProjection?.stop()
        mediaProjection = null
        
        // 注销广播接收器
        try {
            if (screenOffReceiver != null) {
                unregisterReceiver(screenOffReceiver)
                screenOffReceiver = null
            }
            if (uploadStatusReceiver != null) {
                unregisterReceiver(uploadStatusReceiver)
                uploadStatusReceiver = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receivers: ${e.message}")
        }
        
        // 释放WakeLock
        releaseWakeLock()
        
        // 隐藏悬浮窗
        hideFloatingWindow()
    }
}