package com.example.xiaosu.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

/**
 * 网络状态监听器，使用NetworkCallback替代BroadcastReceiver
 * 更加高效且不会错过网络状态变化
 */
class NetworkMonitor(private val context: Context) {
    private val TAG = "NetworkMonitor"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    // 移除对 FileUploadManager 的直接依赖
    private var isRegistered = false
    
    // 定义回调接口
    interface NetworkStatusCallback {
        fun onWifiConnected()
    }
    
    // 回调对象
    private var networkStatusCallback: NetworkStatusCallback? = null
    
    // 设置回调方法
    fun setNetworkStatusCallback(callback: NetworkStatusCallback) {
        this.networkStatusCallback = callback
    }
    
    // 网络回调
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        // 网络可用时回调
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "Network available")
            checkWifiAndUpload(network)
        }
        
        // 网络能力变化时回调
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            Log.d(TAG, "Network capabilities changed")
            checkWifiAndUpload(network)
        }
        
        // 网络丢失时回调
        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d(TAG, "Network lost")
        }
    }
    
    /**
     * 注册网络监听
     */
    fun register() {
        if (isRegistered) return
        
        try {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            isRegistered = true
            Log.d(TAG, "Network monitor registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network callback: ${e.message}")
        }
    }
    
    /**
     * 注销网络监听
     */
    fun unregister() {
        if (!isRegistered) return
        
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isRegistered = false
            Log.d(TAG, "Network monitor unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering network callback: ${e.message}")
        }
    }
    
    /**
     * 检查是否连接到WiFi并上传文件
     */
    private fun checkWifiAndUpload(network: Network) {
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return
        
        // 检查是否连接到WiFi
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            Log.d(TAG, "Connected to WiFi, checking pending uploads")
            // 通过回调通知WiFi已连接
            networkStatusCallback?.onWifiConnected()
        }
    }
    
    /**
     * 检查当前是否连接到WiFi
     */
    fun isConnectedToWifi(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}