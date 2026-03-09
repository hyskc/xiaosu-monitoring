package com.example.xiaosu.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 文件工具类
 * 用于在手机根目录下创建xiaosu文件夹并保存用户数据
 */
object FileUtils {
    private const val TAG = "FileUtils"
    private const val ROOT_DIR_NAME = "xiaosu"
    private var appContext: Context? = null
    
    /**
     * 初始化 FileUtils，设置应用上下文
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    
    /**
     * 获取应用在外部存储的根目录
     * 如果不存在则创建
     */
    fun getRootDirectory(): File? {
        try {
            // 检查应用上下文是否已初始化
            if (appContext == null) {
                Log.e(TAG, "应用上下文未初始化，请先调用 init 方法")
                return null
            }
            
            // 使用应用专属的外部存储目录
            val rootDir = File(appContext!!.getExternalFilesDir(null), ROOT_DIR_NAME)
            
            // 如果目录不存在则创建
            if (!rootDir.exists()) {
                if (!rootDir.mkdirs()) {
                    Log.e(TAG, "创建根目录失败")
                    return null
                }
                Log.d(TAG, "成功创建根目录: ${rootDir.absolutePath}")
            }
            
            return rootDir
        } catch (e: Exception) {
            Log.e(TAG, "获取根目录异常: ${e.message}")
            return null
        }
    }
    
    /**
     * 在根目录下创建子目录
     * @param dirName 子目录名称
     * @return 创建的目录文件对象，如果创建失败则返回null
     */
    fun createDirectory(dirName: String): File? {
        val rootDir = getRootDirectory() ?: return null
        val directory = File(rootDir, dirName)
        
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                Log.e(TAG, "创建目录失败: $dirName")
                return null
            }
            Log.d(TAG, "成功创建目录: ${directory.absolutePath}")
        }
        
        return directory
    }
    
    /**
     * 保存数据到文件
     * @param dirName 目录名称，将在xiaosu根目录下创建
     * @param fileName 文件名称
     * @param data 要保存的数据
     * @return 是否保存成功
     */
    fun saveDataToFile(dirName: String, fileName: String, data: ByteArray): Boolean {
        val directory = createDirectory(dirName) ?: return false
        val file = File(directory, fileName)
        
        return try {
            FileOutputStream(file).use { fos ->
                fos.write(data)
                fos.flush()
            }
            Log.d(TAG, "成功保存文件: ${file.absolutePath}")
            true
        } catch (e: IOException) {
            Log.e(TAG, "保存文件失败: ${e.message}")
            false
        }
    }
    
    /**
     * 保存文本数据到文件
     * @param dirName 目录名称，将在xiaosu根目录下创建
     * @param fileName 文件名称
     * @param text 要保存的文本数据
     * @return 是否保存成功
     */
    fun saveTextToFile(dirName: String, fileName: String, text: String): Boolean {
        return saveDataToFile(dirName, fileName, text.toByteArray())
    }
    
    /**
     * 获取文件路径
     * @param dirName 目录名称
     * @param fileName 文件名称
     * @return 文件路径，如果目录创建失败则返回null
     */
    fun getFilePath(dirName: String, fileName: String): String? {
        val directory = createDirectory(dirName) ?: return null
        return File(directory, fileName).absolutePath
    }
    
    /**
     * 检查文件是否存在
     * @param dirName 目录名称
     * @param fileName 文件名称
     * @return 文件是否存在
     */
    fun isFileExists(dirName: String, fileName: String): Boolean {
        val directory = createDirectory(dirName) ?: return false
        val file = File(directory, fileName)
        return file.exists() && file.isFile
    }
    
    /**
     * 获取缓存目录
     * 优先使用外部缓存目录，如果不可用则使用内部缓存目录
     * @param context 上下文
     * @return 缓存目录
     */
    fun getCacheDirectory(context: Context): File {
        val externalCacheDir = context.externalCacheDir
        return if (externalCacheDir != null && externalCacheDir.exists()) {
            externalCacheDir
        } else {
            context.cacheDir
        }
    }
    
    /**
     * 清除指定目录下的所有文件
     * @param dirName 目录名称
     * @return 是否清除成功
     */
    fun clearDirectory(dirName: String): Boolean {
        val directory = createDirectory(dirName) ?: return false
        return try {
            directory.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
            Log.d(TAG, "成功清除目录: ${directory.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "清除目录失败: ${e.message}")
            false
        }
    }
}