package com.example.xiaosu.network

import android.util.Log
import com.example.xiaosu.config.AppConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // 服务器地址，从AppConfig获取
    const val BASE_URL = AppConfig.SERVER_BASE_URL
    
    // 超时设置，从AppConfig获取
    private val CONNECT_TIMEOUT = AppConfig.API_CONNECT_TIMEOUT
    private val READ_TIMEOUT = AppConfig.API_READ_TIMEOUT
    private val WRITE_TIMEOUT = AppConfig.API_WRITE_TIMEOUT
    
    // 创建OkHttpClient实例
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                try {
                    val response = chain.proceed(request)
                    if (!response.isSuccessful) {
                        Log.e("ApiClient", "API调用失败: ${response.code} ${response.message}, URL: ${request.url}")
                    }
                    response
                } catch (e: Exception) {
                    Log.e("ApiClient", "网络请求异常: ${e.message}, URL: ${request.url}", e)
                    throw e
                }
            }
            .build()
    }
    
    // 创建Retrofit实例
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // 创建API服务接口
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}