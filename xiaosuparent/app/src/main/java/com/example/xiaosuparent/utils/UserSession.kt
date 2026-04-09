package com.example.xiaosuparent.utils

import android.content.Context
import android.content.SharedPreferences

class UserSession private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        
        @Volatile
        private var instance: UserSession? = null
        
        fun getInstance(context: Context): UserSession {
            return instance ?: synchronized(this) {
                instance ?: UserSession(context.applicationContext).also { instance = it }
            }
        }
    }

    // 保存用户登录信息
    fun saveUserLoginInfo(userId: Int, username: String) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putInt(KEY_USER_ID, userId)
        editor.putString(KEY_USERNAME, username)
        editor.apply()
    }

    // 获取用户名
    fun getUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    // 获取用户ID
    fun getUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, -1)
    }

    // 检查用户是否已登录
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // 退出登录
    fun logout() {
        editor.clear()
        editor.apply()
    }
}