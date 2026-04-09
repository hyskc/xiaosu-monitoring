package com.example.xiaosuparent.api

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // 可以在应用启动时动态设置，或者从配置文件读取
    var BASE_URL = "http://192.168.1.241:8081/"
    
    // 创建自定义的Gson实例，确保正确处理isDirectory字段
    private val gson: Gson by lazy {
        GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY) // 使用原始字段名
            .setLenient() // 更宽松的JSON解析
            .create()
    }
    
    // 创建OkHttpClient，配置超时和日志
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .hostnameVerifier { _, _ -> true } // 允许所有主机名
            .sslSocketFactory(createSSLSocketFactory(), TrustAllCerts())
            .build()
    }
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    
    // 用于更新BASE_URL（例如在不同环境切换时）
    fun updateBaseUrl(newBaseUrl: String) {
        if (newBaseUrl != BASE_URL) {
            BASE_URL = newBaseUrl
            // 注意：这不会立即重建retrofit实例，下次访问时才会重建
        }
    }
    
    // 创建信任所有证书的SSLSocketFactory
    private fun createSSLSocketFactory(): javax.net.ssl.SSLSocketFactory {
        return try {
            val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<javax.net.ssl.TrustManager>(TrustAllCerts()), java.security.SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
    
    // 信任所有证书的TrustManager
    private class TrustAllCerts : javax.net.ssl.X509TrustManager {
        override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
    }
}