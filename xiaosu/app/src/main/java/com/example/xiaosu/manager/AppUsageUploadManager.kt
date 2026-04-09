package com.example.xiaosu.manager

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.xiaosu.model.AppUsageInfo
import com.example.xiaosu.network.ApiClient
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class AppUsageUploadManager(private val context: Context) {
    private val TAG = "AppUsageUploadManager"
    private val PREFS_NAME = "app_usage_prefs"
    private val KEY_LAST_UPLOAD_TIME = "last_upload_time"
    private val UPLOAD_INTERVAL = 12 * 60 * 60 * 1000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun scheduleAppUsageUpload() {
        val prefs = getPrefs()
        val lastUploadTime = prefs.getLong(KEY_LAST_UPLOAD_TIME, 0)
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastUploadTime >= UPLOAD_INTERVAL) {
            Log.d(TAG, "Scheduled app usage upload triggered")
        } else {
            Log.d(TAG, "Skipping upload, last upload was at $lastUploadTime")
        }
    }

    fun uploadAppUsageData(appUsageList: List<AppUsageInfo>) {
        if (appUsageList.isEmpty()) {
            Log.d(TAG, "No app usage data to upload")
            return
        }

        Thread {
            try {
                val json = gson.toJson(appUsageList)
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = json.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("${com.example.xiaosu.config.AppConfig.SERVER_BASE_URL}api/app-usage")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "App usage data uploaded successfully")
                        getPrefs().edit().putLong(KEY_LAST_UPLOAD_TIME, System.currentTimeMillis()).apply()
                    } else {
                        Log.e(TAG, "Failed to upload app usage data: ${response.code}")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error uploading app usage data: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}")
            }
        }.start()
    }

    private fun getPrefs(): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}