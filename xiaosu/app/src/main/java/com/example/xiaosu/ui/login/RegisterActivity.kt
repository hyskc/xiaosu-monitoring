package com.example.xiaosu.ui.login

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.xiaosu.R
import com.example.xiaosu.network.ApiClient
import com.example.xiaosu.network.ApiResponse
import com.example.xiaosu.network.RegisterRequest
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var registerButton: Button
    private lateinit var loginTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // 初始化视图
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        registerButton = findViewById(R.id.registerButton)
        loginTextView = findViewById(R.id.loginTextView)

        // 设置点击事件
        setupListeners()
    }

    private fun setupListeners() {
        // 注册按钮点击事件
        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_input_username_password), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, getString(R.string.password_not_match), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 调用注册API
            register(username, password)
        }

        // 登录文本点击事件，返回登录界面
        loginTextView.setOnClickListener {
            finish() // 关闭当前界面，返回登录界面
        }
    }

    private fun register(username: String, password: String) {
        // 显示加载中提示
        registerButton.isEnabled = false
        registerButton.text = getString(R.string.registering)

        val registerRequest = RegisterRequest(username, password)

        ApiClient.apiService.register(registerRequest).enqueue(object : Callback<ApiResponse<Void>> {
            override fun onResponse(
                call: Call<ApiResponse<Void>>,
                response: Response<ApiResponse<Void>>
            ) {
                registerButton.isEnabled = true
                registerButton.text = getString(R.string.register)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.success) {
                        // 注册成功
                        Toast.makeText(this@RegisterActivity, apiResponse.message, Toast.LENGTH_LONG).show()
                        
                        // 返回登录界面
                        finish()
                    } else {
                        // 注册失败
                        Toast.makeText(
                            this@RegisterActivity,
                            apiResponse?.message ?: getString(R.string.register_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // 请求失败
                    Toast.makeText(this@RegisterActivity, getString(R.string.network_request_failed), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Void>>, t: Throwable) {
                registerButton.isEnabled = true
                registerButton.text = getString(R.string.register)
                // 网络错误
                Toast.makeText(this@RegisterActivity, getString(R.string.network_error, t.message), Toast.LENGTH_SHORT).show()
            }
        })
    }
}