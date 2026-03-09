package com.example.xiaosu.util

import android.content.Context
import android.os.Process
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日志工具类
 * 用于记录应用错误日志和意外关闭信息
 */
object LogUtils {
    private const val TAG = "LogUtils"
    private const val LOG_DIR_NAME = "logs"
    private const val ERROR_LOG_FILE_NAME = "error_log.txt"
    private const val MAX_LOG_SIZE = 1024 * 1024 * 5 // 5MB
    private const val MAX_LOG_COUNT = 100 // 最多保存100条日志
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val logQueue = LinkedList<String>()
    
    /**
     * 初始化日志工具，设置全局未捕获异常处理器
     */
    fun init(context: Context) {
        // 读取已有的日志
        readExistingLogs()
        
        // 设置全局未捕获异常处理器
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logException(throwable)
            // 调用默认的异常处理器
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        // 记录应用启动信息
        logInfo("应用启动 - PID: ${Process.myPid()}")
    }
    
    /**
     * 记录普通信息日志
     */
    fun logInfo(message: String) {
        val logMessage = "[INFO] ${getCurrentTime()} - $message"
        Log.i(TAG, logMessage)
        addLogToQueue(logMessage)
        saveLogsToFile()
    }
    
    /**
     * 记录错误信息日志
     */
    fun logError(message: String) {
        val logMessage = "[ERROR] ${getCurrentTime()} - $message"
        Log.e(TAG, logMessage)
        addLogToQueue(logMessage)
        saveLogsToFile()
    }
    
    /**
     * 记录异常信息
     */
    fun logException(throwable: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()
        
        val logMessage = "[EXCEPTION] ${getCurrentTime()} - ${throwable.message}\n$stackTrace"
        Log.e(TAG, logMessage)
        addLogToQueue(logMessage)
        saveLogsToFile()
    }
    
    /**
     * 获取所有日志内容
     */
    fun getAllLogs(): String {
        return logQueue.joinToString("\n\n")
    }
    
    /**
     * 清除所有日志
     */
    fun clearLogs() {
        logQueue.clear()
        val logDir = FileUtils.createDirectory(LOG_DIR_NAME)
        if (logDir != null) {
            val logFile = File(logDir, ERROR_LOG_FILE_NAME)
            if (logFile.exists()) {
                logFile.delete()
            }
        }
        logInfo("日志已清除")
    }
    
    /**
     * 获取当前时间字符串
     */
    private fun getCurrentTime(): String {
        return dateFormat.format(Date())
    }
    
    /**
     * 添加日志到队列
     */
    private fun addLogToQueue(logMessage: String) {
        logQueue.add(logMessage)
        // 限制日志数量
        while (logQueue.size > MAX_LOG_COUNT) {
            logQueue.removeFirst()
        }
    }
    
    /**
     * 读取已有的日志文件
     */
    private fun readExistingLogs() {
        try {
            val logDir = FileUtils.createDirectory(LOG_DIR_NAME)
            if (logDir != null) {
                val logFile = File(logDir, ERROR_LOG_FILE_NAME)
                if (logFile.exists() && logFile.length() > 0) {
                    val logs = logFile.readText()
                    if (logs.isNotEmpty()) {
                        // 按日志条目分割并添加到队列
                        logs.split("\n\n").filter { it.isNotEmpty() }.forEach { log ->
                            addLogToQueue(log)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "读取已有日志失败: ${e.message}")
        }
    }
    
    /**
     * 保存日志到文件
     */
    private fun saveLogsToFile() {
        try {
            val logDir = FileUtils.createDirectory(LOG_DIR_NAME)
            if (logDir != null) {
                val logFile = File(logDir, ERROR_LOG_FILE_NAME)
                
                // 检查文件大小，如果超过最大大小，则备份并创建新文件
                if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                    val backupFile = File(logDir, "${ERROR_LOG_FILE_NAME}.bak")
                    if (backupFile.exists()) {
                        backupFile.delete()
                    }
                    logFile.renameTo(backupFile)
                }
                
                // 写入所有日志
                FileOutputStream(logFile).use { fos ->
                    fos.write(getAllLogs().toByteArray())
                    fos.flush()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存日志到文件失败: ${e.message}")
        }
    }
}