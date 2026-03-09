package com.example.xiaosuparent.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.xiaosuparent.R
import com.example.xiaosuparent.api.RetrofitClient
import com.example.xiaosuparent.model.LoginRequest
import com.example.xiaosuparent.utils.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginFragment : Fragment() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化视图
        usernameEditText = view.findViewById(R.id.editText_username)
        passwordEditText = view.findViewById(R.id.editText_password)
        loginButton = view.findViewById(R.id.button_login)
        registerTextView = view.findViewById(R.id.textView_register)

        // 设置登录按钮点击事件
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // 简单的验证
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "用户名和密码不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 显示加载中提示
            Toast.makeText(requireContext(), "登录中...", Toast.LENGTH_SHORT).show()
            
            // 调用登录API
            val loginRequest = LoginRequest(username, password)
            RetrofitClient.apiService.login(loginRequest).enqueue(object : Callback<com.example.xiaosuparent.model.ApiResponse<com.example.xiaosuparent.model.LoginResponse>> {
                override fun onResponse(
                    call: Call<com.example.xiaosuparent.model.ApiResponse<com.example.xiaosuparent.model.LoginResponse>>,
                    response: Response<com.example.xiaosuparent.model.ApiResponse<com.example.xiaosuparent.model.LoginResponse>>
                ) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.code == 200 && apiResponse.data != null) {
                            // 登录成功，保存用户信息
                            val loginResponse = apiResponse.data
                            val userSession = UserSession.getInstance(requireContext())
                            loginResponse.userId?.let { userId ->
                                loginResponse.username?.let { username ->
                                    userSession.saveUserLoginInfo(userId, username)
                                }
                            }
                            
                            Toast.makeText(requireContext(), apiResponse.message, Toast.LENGTH_SHORT).show()
                            // 登录成功后跳转到首页
                            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                        } else {
                            // 登录失败
                            Toast.makeText(requireContext(), apiResponse?.message ?: "登录失败", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // 请求失败
                        Toast.makeText(requireContext(), "网络请求失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<com.example.xiaosuparent.model.ApiResponse<com.example.xiaosuparent.model.LoginResponse>>, t: Throwable) {
                    // 网络错误
                    Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // 设置注册文本点击事件
        registerTextView.setOnClickListener {
            // 跳转到注册页面
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }
}