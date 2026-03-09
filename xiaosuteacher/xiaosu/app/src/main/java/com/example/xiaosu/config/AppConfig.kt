package com.example.xiaosu.config

/**
 * 应用程序配置类
 * 集中管理应用程序的配置信息，如服务器地址等
 */
object AppConfig {
    // 服务器IP和端口（便于快速修改）
    const val SERVER_IP = "47.97.65.168"
    const val SERVER_PORT = "8082"
    //192.168.124.79
    //47.97.65.168
    // 服务器基础URL
    const val SERVER_BASE_URL = "http://$SERVER_IP:$SERVER_PORT/"
    
    // API相关超时设置
    const val API_CONNECT_TIMEOUT = 30L // 连接超时时间（秒）
    const val API_READ_TIMEOUT = 30L    // 读取超时时间（秒）
    const val API_WRITE_TIMEOUT = 60L   // 写入超时时间（秒）
    
    // API路径
    const val API_LOGIN_PATH = "api/students/login"
    const val API_REGISTER_PATH = "api/students/register"
    const val API_UPLOAD_PATH = "api/upload"
    const val API_AVATAR_PATH = "api/students/avatar"
    const val API_ASSOCIATION_PATH = "api/association"
    const val API_VALIDATE_PARENT_CODE_PATH = "api/parents/validate-code"
}

// 其他配置常量可以在此添加
