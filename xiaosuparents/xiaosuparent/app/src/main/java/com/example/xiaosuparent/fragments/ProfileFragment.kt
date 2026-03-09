package com.example.xiaosuparent.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.xiaosuparent.R
import com.example.xiaosuparent.utils.UserSession

class ProfileFragment : Fragment() {

    private lateinit var titleTextView: TextView
    private lateinit var usernameTextView: TextView
    private lateinit var loginButton: Button
    private lateinit var logoutButton: RadioButton
    private lateinit var linkStudentButton: RadioButton
    private lateinit var viewAssociatedStudentsButton: RadioButton
    private lateinit var settingsRadioGroup: RadioGroup
    private lateinit var loggedInLayout: View
    private lateinit var userSession: UserSession

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化用户会话
        userSession = UserSession.getInstance(requireContext())

        // 初始化视图
        titleTextView = view.findViewById(R.id.textView_profile_title)
        usernameTextView = view.findViewById(R.id.textView_username)
        loginButton = view.findViewById(R.id.button_login_register)
        settingsRadioGroup = view.findViewById(R.id.radioGroup_settings)
        logoutButton = view.findViewById(R.id.button_logout)
        linkStudentButton = view.findViewById(R.id.button_link_student)
        viewAssociatedStudentsButton = view.findViewById(R.id.button_view_associated_students)
        loggedInLayout = view.findViewById(R.id.layout_logged_in)

        // 设置登录/注册按钮点击事件
        loginButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }

        // 设置RadioGroup选择事件
        settingsRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.button_link_student -> {
                    findNavController().navigate(R.id.action_profileFragment_to_linkStudentFragment)
                    // 重置选择状态
                    group.clearCheck()
                }
                R.id.button_view_associated_students -> {
                    findNavController().navigate(R.id.action_profileFragment_to_associatedStudentsFragment)
                    // 重置选择状态
                    group.clearCheck()
                }
                R.id.button_logout -> {
                    userSession.logout()
                    updateUI()
                    // 重置选择状态
                    group.clearCheck()
                }
            }
        }

        // 更新UI显示
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        // 每次恢复时更新UI
        updateUI()
    }

    private fun updateUI() {
        if (userSession.isLoggedIn()) {
            // 已登录状态
            loginButton.visibility = View.GONE
            loggedInLayout.visibility = View.VISIBLE
            usernameTextView.text = "用户名：${userSession.getUsername()}"
        } else {
            // 未登录状态
            loginButton.visibility = View.VISIBLE
            loggedInLayout.visibility = View.GONE
        }
    }
}