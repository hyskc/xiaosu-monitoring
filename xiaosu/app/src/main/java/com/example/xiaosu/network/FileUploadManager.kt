package com.example.xiaosu.network

import android.content.Context
import android.content.Intent
import android.util.Log
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.concurrent.ConcurrentLinkedQueue
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import com.example.xiaosu.config.AppConfig

class FileUploadManager(private val context: Context) {
    private val TAG = "FileUploadManager"
    private val SERVER_URL = "${AppConfig.SERVER_BASE_URL}api/upload"
    private val CHUNK_SIZE = 1024 * 1024 // 1MB
    private val client = OkHttpClient.Builder()
        .connectTimeout(AppConfig.API_CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(AppConfig.API_WRITE_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(AppConfig.API_READ_TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    // 移除对 NetworkMonitor 的直接依赖
    
    // 待上传文件队列
    private val pendingUploads = ConcurrentLinkedQueue<File>()
    private var isUploadingPendingFiles = false

    // 上传文件，支持断点续传
    fun uploadFile(file: File) {
        Thread {
            try {
                // 检查是否使用数据网络上传
                if (!isWifiOnlyUploadEnabled() && !isConnectedToWifi()) {
                    Log.d(TAG, "WiFi only upload enabled and not on WiFi, storing file for later: ${file.name}")
                    addToPendingUploads(file)
                    return@Thread
                }
                
                val uploadInfo = getUploadInfo(file)
                if (uploadInfo.uploaded >= file.length()) {
                    Log.d(TAG, "File already uploaded: ${file.name}")
                    // 文件已上传，删除本地文件
                    deleteLocalFile(file)
                    return@Thread
                }

                // 尝试上传文件，只尝试一次
                Log.d(TAG, "Attempting to upload file: ${file.name}")
                
                val uploadSuccess = uploadFileWithResume(file, uploadInfo.uploaded)
                
                if (uploadSuccess) {
                    // 上传成功，删除本地文件
                    deleteLocalFile(file)
                } else {
                    Log.e(TAG, "Failed to upload file: ${file.name}")
                    // 通知服务器删除可能损坏的文件
                    deleteCorruptedFile(file.name, getUserId())
                    // 不再重试上传，也不添加到待上传队列
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading file: ${e.message}")
                // 不再保存上传状态或添加到待上传队列
                // 清理可能存在的临时块文件
                cleanupTempChunkFiles(file.name)
            }
        }.start()
    }
    
    // 检查是否使用数据网络上传
    private fun isWifiOnlyUploadEnabled(): Boolean {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("wifi_only_upload", true) // 默认只在WiFi下上传
    }
    
    // 检查当前是否连接到WiFi
    private fun isConnectedToWifi(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    // 添加到待上传队列
    private fun addToPendingUploads(file: File) {
        // 检查文件是否已在队列中
        if (!pendingUploads.contains(file)) {
            pendingUploads.add(file)
            savePendingUploads()
        }
    }
    
    // 保存待上传队列
    private fun savePendingUploads() {
        val prefs = context.getSharedPreferences("pending_uploads", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val filePathSet = pendingUploads.map { it.absolutePath }.toSet()
        editor.putStringSet("pending_files", filePathSet)
        editor.apply()
    }
    
    // 加载待上传队列
    private fun loadPendingUploads() {
        val prefs = context.getSharedPreferences("pending_uploads", Context.MODE_PRIVATE)
        val filePathSet = prefs.getStringSet("pending_files", emptySet()) ?: emptySet()
        pendingUploads.clear()
        filePathSet.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                pendingUploads.add(file)
            }
        }
    }
    
    // 检查网络状态变化，在WIFI连接时上传待上传文件
    fun checkNetworkAndUploadPending() {
        if (!isWifiOnlyUploadEnabled() || isConnectedToWifi()) {
            uploadPendingFiles();
        }
    }
    
    // 上传待上传队列中的文件
    private fun uploadPendingFiles() {
        if (pendingUploads.isEmpty() || isUploadingPendingFiles) {
            return
        }
        
        isUploadingPendingFiles = true
        
        Thread {
            try {
                // 加载待上传队列
                loadPendingUploads()
                
                while (pendingUploads.isNotEmpty() && (!isWifiOnlyUploadEnabled() || isConnectedToWifi())) {
                    val file = pendingUploads.poll()
                    if (file != null && file.exists()) {
                        Log.d(TAG, "Uploading pending file: ${file.name}")
                        val uploadInfo = getUploadInfo(file)
                        if (uploadInfo.uploaded >= file.length()) {
                            // 文件已上传，删除本地文件
                            deleteLocalFile(file)
                        } else {
                            val uploadSuccess = uploadFileWithResume(file, uploadInfo.uploaded)
                            if (uploadSuccess) {
                                // 上传成功，删除本地文件
                                deleteLocalFile(file)
                            } else {
                                Log.e(TAG, "Failed to upload pending file: ${file.name}")
                                // 通知服务器删除可能损坏的文件
                                deleteCorruptedFile(file.name, getUserId())
                                // 清理可能存在的临时块文件
                                cleanupTempChunkFiles(file.name)
                                // 不再重试上传，直接处理下一个文件
                            }
                        }
                    }
                    savePendingUploads() // 更新待上传队列
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading pending files: ${e.message}")
            } finally {
                isUploadingPendingFiles = false
            }
        }.start()
    }

    // 获取已上传的信息
    private fun getUploadInfo(file: File): UploadInfo {
        try {
            // 获取用户ID
            val userId = getUserId()
            if (userId <= 0) {
                Log.e(TAG, "User not logged in, cannot upload file")
                return UploadInfo(0)
            }
            
            // 检查服务器上已上传的部分
            val request = Request.Builder()
                .url("$SERVER_URL/status?fileName=${file.name}&fileSize=${file.length()}&userId=$userId")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    Log.d(TAG, "Upload status response: $body")
                    
                    // 解析JSON响应
                    if (body != null && body.contains("uploaded")) {
                        // 简单解析JSON，提取uploaded字段
                        val uploadedStr = body.substringAfter("\"uploaded\":").substringBefore(",")
                        val uploaded = uploadedStr.trim().toLongOrNull() ?: 0L
                        return UploadInfo(uploaded)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking upload status: ${e.message}")
        }

        // 如果无法从服务器获取信息，则从本地获取
        return UploadInfo(getLocalUploadedBytes(file))
    }

    // 从本地获取已上传的字节数
    private fun getLocalUploadedBytes(file: File): Long {
        val prefs = context.getSharedPreferences("upload_state", Context.MODE_PRIVATE)
        return prefs.getLong(file.name, 0L)
    }

    // 保存上传状态
    private fun saveUploadState(file: File, uploadedBytes: Long) {
        val prefs = context.getSharedPreferences("upload_state", Context.MODE_PRIVATE)
        prefs.edit().putLong(file.name, uploadedBytes).apply()
    }
    
    // 获取当前登录用户ID
    private fun getUserId(): Int {
        val sharedPreferences = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("user_id", -1)
    }

    // 使用断点续传上传文件
    private fun uploadFileWithResume(file: File, startPosition: Long): Boolean {
        var currentPosition = startPosition
        val fileLength = file.length()
        
        // 获取用户ID
        val userId = getUserId()
        if (userId <= 0) {
            Log.e(TAG, "User not logged in, cannot upload file")
            return false
        }

        try {
            val randomAccessFile = RandomAccessFile(file, "r")
            randomAccessFile.seek(currentPosition)

            while (currentPosition < fileLength) {
                // 计算当前块的大小
                val endPosition = minOf(currentPosition + CHUNK_SIZE, fileLength)
                val chunkSize = (endPosition - currentPosition).toInt()
                val buffer = ByteArray(chunkSize)

                // 读取数据块
                randomAccessFile.read(buffer)

                // 创建临时文件存储当前块
                val chunkFile = File(context.cacheDir, "${file.name}_chunk_${currentPosition}")
                chunkFile.writeBytes(buffer)

                try {
                    // 上传数据块
                    val success = uploadChunk(chunkFile, file.name, currentPosition, fileLength, userId)
                    
                    // 无论上传成功与否，都删除临时文件
                    if (chunkFile.exists()) {
                        chunkFile.delete()
                        Log.d(TAG, "Deleted temp chunk file: ${chunkFile.name}")
                    }

                    if (success) {
                        currentPosition = endPosition
                        saveUploadState(file, currentPosition)
                        Log.d(TAG, "Uploaded ${currentPosition * 100 / fileLength}% of ${file.name}")
                    } else {
                        // 上传失败，不再保存状态，直接返回失败
                        Log.e(TAG, "Failed to upload chunk at position $currentPosition")
                        randomAccessFile.close()
                        // 清理所有临时块文件
                        cleanupTempChunkFiles(file.name)
                        return false
                    }
                } catch (e: Exception) {
                    // 上传过程中出现异常，确保删除临时文件
                    if (chunkFile.exists()) {
                        chunkFile.delete()
                        Log.d(TAG, "Deleted temp chunk file after exception: ${chunkFile.name}")
                    }
                    throw e
                }
            }

            randomAccessFile.close()

            if (currentPosition >= fileLength) {
                // 通知服务器文件上传完成
                if (completeUpload(file.name, fileLength, userId)) {
                    // 验证文件完整性
                    if (verifyFileIntegrity(file.name, fileLength, userId)) {
                        Log.d(TAG, "File uploaded and verified successfully: ${file.name}")
                        // 上传完成后，清除上传状态
                        saveUploadState(file, 0)
                        // 发送广播通知上传完成
                        sendUploadCompleteBroadcast(file.name, true)
                        return true
                    } else {
                        Log.e(TAG, "File integrity check failed for ${file.name}")
                        // 不保存上传状态
                        // 发送广播通知上传失败
                        sendUploadCompleteBroadcast(file.name, false)
                        return false
                    }
                } else {
                    Log.e(TAG, "Failed to complete upload for ${file.name}")
                    // 不保存当前上传状态
                    return false
                }
            }
            return false
        } catch (e: IOException) {
            Log.e(TAG, "Error during file upload: ${e.message}")
            // 不保存上传状态
            // 清理所有临时块文件
            cleanupTempChunkFiles(file.name)
            return false
        }
    }

    // 上传单个数据块
    private fun uploadChunk(chunkFile: File, originalFileName: String, position: Long, totalSize: Long, userId: Int): Boolean {
        try {
            // 计算当前块的索引和总块数
            val chunkSize = CHUNK_SIZE.toLong()
            val totalChunks = (totalSize + chunkSize - 1) / chunkSize // 向上取整
            val chunkIndex = position / chunkSize
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("fileName", originalFileName)
                .addFormDataPart("chunkIndex", chunkIndex.toString())
                .addFormDataPart("chunkTotal", totalChunks.toString())
                .addFormDataPart("startByte", position.toString())
                .addFormDataPart("userId", userId.toString())
                .addFormDataPart("filePart", originalFileName + "_chunk", chunkFile.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
                .build()

            val request = Request.Builder()
                .url("$SERVER_URL/chunk")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val isSuccess = response.isSuccessful
                if (isSuccess) {
                    Log.d(TAG, "Successfully uploaded chunk $chunkIndex of $totalChunks")
                } else {
                    Log.e(TAG, "Failed to upload chunk: ${response.code} ${response.message}")
                }
                return isSuccess
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading chunk: ${e.message}")
            return false
        }
    }

    // 上传信息数据类
    data class UploadInfo(val uploaded: Long)
    
    // 通知服务器文件上传完成
    private fun completeUpload(fileName: String, fileSize: Long, userId: Int): Boolean {
        try {
            // 创建一个包含至少一个部分的MultipartBody
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("fileName", fileName)
                .addFormDataPart("fileSize", fileSize.toString())
                .addFormDataPart("userId", userId.toString())
                .build()
                
            val request = Request.Builder()
                .url("$SERVER_URL/complete")
                .post(requestBody)
                .build()
                
            client.newCall(request).execute().use { response ->
                val isSuccess = response.isSuccessful
                if (isSuccess) {
                    Log.d(TAG, "Successfully completed upload for $fileName")
                } else {
                    Log.e(TAG, "Failed to complete upload: ${response.code} ${response.message}")
                }
                return isSuccess
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error completing upload: ${e.message}")
            return false
        }
    }
    
    // 验证文件完整性
    private fun verifyFileIntegrity(fileName: String, fileSize: Long, userId: Int): Boolean {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("fileName", fileName)
                .addFormDataPart("fileSize", fileSize.toString())
                .addFormDataPart("userId", userId.toString())
                .build()
                
            val request = Request.Builder()
                .url("$SERVER_URL/verify")
                .post(requestBody)
                .build()
                
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        // 解析响应，检查文件是否完整
                        val isIntact = body.contains("\"intact\":true")
                        Log.d(TAG, "File integrity check result for $fileName: $isIntact")
                        return isIntact
                    }
                }
                Log.e(TAG, "Failed to verify file integrity: ${response.code} ${response.message}")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying file integrity: ${e.message}")
            return false
        }
    }
    
    // 通知服务器删除损坏的文件
    private fun deleteCorruptedFile(fileName: String, userId: Int): Boolean {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("fileName", fileName)
                .addFormDataPart("userId", userId.toString())
                .build()
                
            val request = Request.Builder()
                .url("$SERVER_URL/delete-corrupted")
                .post(requestBody)
                .build()
                
            client.newCall(request).execute().use { response ->
                val isSuccess = response.isSuccessful
                if (isSuccess) {
                    Log.d(TAG, "Successfully requested deletion of corrupted file: $fileName")
                } else {
                    Log.e(TAG, "Failed to request deletion of corrupted file: ${response.code} ${response.message}")
                }
                return isSuccess
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting deletion of corrupted file: ${e.message}")
            return false
        }
    }
    
    // 发送上传完成广播
    private fun sendUploadCompleteBroadcast(fileName: String, success: Boolean) {
        val intent = Intent(ACTION_UPLOAD_COMPLETE)
        intent.putExtra(EXTRA_FILE_NAME, fileName)
        intent.putExtra(EXTRA_UPLOAD_SUCCESS, success)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
    
    // 删除本地视频文件
    private fun deleteLocalFile(file: File) {
        if (file.exists()) {
            try {
                if (file.delete()) {
                    Log.d(TAG, "Successfully deleted local file: ${file.name}")
                } else {
                    Log.e(TAG, "Failed to delete local file: ${file.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting local file ${file.name}: ${e.message}")
            }
        }
    }
    
    // 清理临时块文件
    private fun cleanupTempChunkFiles(fileName: String) {
        try {
            val cacheDir = context.cacheDir
            val chunkFiles = cacheDir.listFiles { file -> 
                file.name.startsWith("${fileName}_chunk_") 
            }
            
            chunkFiles?.forEach { chunkFile ->
                if (chunkFile.exists()) {
                    if (chunkFile.delete()) {
                        Log.d(TAG, "Deleted temp chunk file: ${chunkFile.name}")
                    } else {
                        Log.e(TAG, "Failed to delete temp chunk file: ${chunkFile.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up temp chunk files: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "FileUploadManager"
        private const val CHUNK_SIZE = 1024 * 1024 // 1MB
        private const val PREFS_NAME = "upload_prefs"
        private const val PENDING_UPLOADS_KEY = "pending_uploads"
        
        // 广播相关常量
        const val ACTION_UPLOAD_COMPLETE = "com.example.xiaosu.ACTION_UPLOAD_COMPLETE"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_UPLOAD_SUCCESS = "upload_success"
    }
}