package com.example.xiaosu.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.xiaosu.MainActivity
import com.example.xiaosu.R
import com.example.xiaosu.network.ApiClient
import com.example.xiaosu.network.ApiResponse
import com.example.xiaosu.network.LoginRequest
import com.example.xiaosu.network.StudentResponse
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var registerTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 初始化视图
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerTextView = findViewById(R.id.registerTextView)

        // 设置点击事件
        setupListeners()
    }

    private fun setupListeners() {
        // 登录按钮点击事件
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_input_username_password), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 调用登录API
            login(username, password)
        }

        // 注册文本点击事件，跳转到注册界面
        registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun login(username: String, password: String) {
        // 显示加载中提示
        loginButton.isEnabled = false
        loginButton.text = getString(R.string.logging_in)

        val loginRequest = LoginRequest(username, password)

        ApiClient.apiService.login(loginRequest).enqueue(object : Callback<ApiResponse<StudentResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<StudentResponse>>,
                response: Response<ApiResponse<StudentResponse>>
            ) {
                loginButton.isEnabled = true
                loginButton.text = getString(R.string.login)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.success) {
                        // 登录成功
                        Toast.makeText(this@LoginActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                        
                        // 保存登录状态和用户信息
                        saveLoginState(apiResponse.data)
                        
                        // 跳转到主界面
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // 登录失败
                        Toast.makeText(
                            this@LoginActivity,
                            apiResponse?.message ?: "登录失败",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // 请求失败
                    Toast.makeText(this@LoginActivity, getString(R.string.network_request_failed), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<StudentResponse>>, t: Throwable) {
                loginButton.isEnabled = true
                loginButton.text = getString(R.string.login)
                // 网络错误
                Toast.makeText(this@LoginActivity, getString(R.string.network_error, t.message), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveLoginState(studentResponse: StudentResponse?) {
        if (studentResponse == null) return
        
        // 使用SharedPreferences保存登录状态和用户信息
        val sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("is_logged_in", true)
        editor.putInt("user_id", studentResponse.id)
        editor.putString("username", studentResponse.username)
        editor.putBoolean("is_active", studentResponse.active)
        editor.apply()
    }
}