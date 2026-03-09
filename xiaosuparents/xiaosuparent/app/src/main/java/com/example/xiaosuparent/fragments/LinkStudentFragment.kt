package com.example.xiaosuparent.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.xiaosuparent.R
import com.example.xiaosuparent.api.ApiService
import com.example.xiaosuparent.api.RetrofitClient
import com.example.xiaosuparent.model.AssociationRequest
import com.example.xiaosuparent.utils.UserSession
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.WeakReference

class LinkStudentFragment : Fragment() {
    private lateinit var studentAccountEditText: TextInputEditText
    private lateinit var studentPasswordEditText: TextInputEditText
    private lateinit var linkButton: Button
    private lateinit var viewAssociatedStudentsButton: Button
    private lateinit var linkStatusTextView: TextView
    private lateinit var backButton: View
    private var handler: Handler? = null
    private var navigationRunnable: Runnable? = null
    private val userSession by lazy { UserSession.getInstance(requireContext()) }
    private val apiService by lazy { RetrofitClient.apiService }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_link_student, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化Handler
        handler = Handler(Looper.getMainLooper())

        // 初始化视图
        studentAccountEditText = view.findViewById(R.id.editText_student_account)
        studentPasswordEditText = view.findViewById(R.id.editText_student_password)
        linkButton = view.findViewById(R.id.button_link_student)
        viewAssociatedStudentsButton = view.findViewById(R.id.button_view_associated_students)
        linkStatusTextView = view.findViewById(R.id.textView_link_status)
        backButton = view.findViewById(R.id.button_back)
        
        // 设置返回按钮点击事件
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // 设置关联按钮点击事件
        linkButton.setOnClickListener {
            val studentUsername = studentAccountEditText.text.toString().trim()
            val studentPassword = studentPasswordEditText.text.toString().trim()

            if (studentUsername.isEmpty() || studentPassword.isEmpty()) {
                linkStatusTextView.text = "请输入学生账号和密码"
                linkStatusTextView.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // 检查用户是否登录
            if (!userSession.isLoggedIn()) {
                linkStatusTextView.text = "请先登录"
                linkStatusTextView.visibility = View.VISIBLE

                // 使用Handler延迟导航到登录页面
                val runnable = Runnable {
                    try {
                        if (!isDetached) {
                            findNavController().navigate(R.id.action_linkStudentFragment_to_loginFragment)
                        }
                    } catch (e: Exception) {
                        // 忽略导航异常
                    }
                }
                navigationRunnable = runnable
                handler?.postDelayed(runnable, 1500)
                return@setOnClickListener
            }

            associateStudent(studentUsername, studentPassword)
        }

        // 设置查看已关联学生账号按钮点击事件
        viewAssociatedStudentsButton.setOnClickListener {
            // 检查用户是否登录
            if (!userSession.isLoggedIn()) {
                linkStatusTextView.text = "请先登录"
                linkStatusTextView.visibility = View.VISIBLE

                // 使用Handler延迟导航到登录页面
                val runnable = Runnable {
                    try {
                        if (!isDetached) {
                            findNavController().navigate(R.id.action_linkStudentFragment_to_loginFragment)
                        }
                    } catch (e: Exception) {
                        // 忽略导航异常
                    }
                }
                navigationRunnable = runnable
                handler?.postDelayed(runnable, 1500)
                return@setOnClickListener
            }

            // 导航到已关联学生账号界面
            try {
                if (!isDetached) {
                    findNavController().navigate(R.id.action_linkStudentFragment_to_associatedStudentsFragment)
                }
            } catch (e: Exception) {
                // 忽略导航异常
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 移除所有回调并清空handler引用，防止内存泄漏
        val localNavigationRunnable = navigationRunnable
        if (localNavigationRunnable != null) {
            handler?.removeCallbacks(localNavigationRunnable)
        }
        handler = null
        navigationRunnable = null
    }

    private fun associateStudent(studentUsername: String, studentPassword: String) {
        linkStatusTextView.text = "正在关联..."
        linkStatusTextView.visibility = View.VISIBLE

        val parentId = userSession.getUserId()
        if (parentId == -1) {
            linkStatusTextView.text = "用户ID获取失败，请重新登录"
            return
        }

        val request = AssociationRequest(parentId, studentUsername, studentPassword)
        val call = apiService.associateStudent(request)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                // 检查Fragment是否已分离
                if (isDetached) return

                if (response.isSuccessful) {
                    linkStatusTextView.text = "关联成功"
                    // 清空输入框
                    studentAccountEditText.text?.clear()
                    studentPasswordEditText.text?.clear()

                    // 使用Handler延迟返回上一页面
                    val runnable = Runnable {
                        try {
                            if (!isDetached) {
                                findNavController().popBackStack()
                            }
                        } catch (e: Exception) {
                            // 忽略导航异常
                        }
                    }
                    navigationRunnable = runnable
                    handler?.postDelayed(runnable, 1500)
                } else {
                    when (response.code()) {
                        400 -> linkStatusTextView.text = "关联失败：请求参数错误"
                        401 -> linkStatusTextView.text = "关联失败：学生账号或密码错误"
                        409 -> linkStatusTextView.text = "关联失败：该学生账号已被关联"
                        else -> linkStatusTextView.text = "关联失败：${response.code()}"
                    }
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // 检查Fragment是否已分离
                if (isDetached) return

                linkStatusTextView.text = "关联失败：${t.message}"
            }
        })
    }
}