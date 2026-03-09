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

class RegisterFragment : Fragment() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var backToLoginTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化视图
        usernameEditText = view.findViewById(R.id.editText_register_username)
        passwordEditText = view.findViewById(R.id.editText_register_password)
        confirmPasswordEditText = view.findViewById(R.id.editText_register_confirm_password)
        registerButton = view.findViewById(R.id.button_register)
        backToLoginTextView = view.findViewById(R.id.textView_back_to_login)

        // 设置注册按钮点击事件
        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            // 简单的验证
            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "所有字段都不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 模拟注册成功
            Toast.makeText(requireContext(), "注册成功，请登录", Toast.LENGTH_SHORT).show()
            // 注册成功后返回登录页面
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        // 设置返回登录文本点击事件
        backToLoginTextView.setOnClickListener {
            // 返回登录页面
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }
}