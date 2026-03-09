package com.example.xiaosuparent.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xiaosuparent.R
import com.example.xiaosuparent.model.ApiResponse
import com.example.xiaosuparent.api.RetrofitClient
import com.example.xiaosuparent.model.Student
import com.example.xiaosuparent.utils.UserSession
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AssociatedStudentsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noStudentsTextView: TextView
    private lateinit var progressBar: View
    private lateinit var userSession: UserSession
    private lateinit var adapter: StudentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_associated_students, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userSession = UserSession.getInstance(requireContext())
        
        // 初始化视图
        recyclerView = view.findViewById(R.id.recyclerView_students)
        noStudentsTextView = view.findViewById(R.id.textView_no_students)
        progressBar = view.findViewById(R.id.progressBar)
        
        // 设置返回按钮
        view.findViewById<View>(R.id.button_back).setOnClickListener {
            findNavController().navigateUp()
        }
        
        // 设置添加学生按钮
        view.findViewById<View>(R.id.fab_add_student).setOnClickListener {
            findNavController().navigate(R.id.action_associatedStudentsFragment_to_linkStudentFragment)
        }

        // 设置RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = StudentAdapter(mutableListOf()) { student ->
            showUnlinkConfirmDialog(student)
        }
        recyclerView.adapter = adapter

        // 检查用户是否登录
        if (!userSession.isLoggedIn()) {
            Snackbar.make(view, "请先登录", Snackbar.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_associatedStudentsFragment_to_loginFragment)
            return
        }

        // 加载关联的学生
        loadAssociatedStudents()
    }

    private fun loadAssociatedStudents() {
        val parentId = userSession.getUserId()
        if (parentId == -1) {
            showNoStudentsMessage()
            return
        }

        showLoading()
        RetrofitClient.apiService.getAssociatedStudents(parentId).enqueue(object : Callback<ApiResponse<List<Student>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<Student>>>,
                response: Response<ApiResponse<List<Student>>>
            ) {
                hideLoading()
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        val students = apiResponse.data ?: emptyList()
                        if (students.isEmpty()) {
                            showNoStudentsMessage()
                        } else {
                            showStudentsList(students)
                        }
                    } else {
                        showError(apiResponse?.message ?: "加载失败")
                    }
                } else {
                    showError("加载失败: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Student>>>, t: Throwable) {
                hideLoading()
                showError("网络错误: ${t.message}")
            }
        })
    }

    private fun showUnlinkConfirmDialog(student: Student) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.unlink))
            .setMessage(getString(R.string.unlink_confirm))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                unlinkStudent(student)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun unlinkStudent(student: Student) {
        val parentId = userSession.getUserId()
        if (parentId == -1) {
            showError("用户ID无效")
            return
        }

        showLoading()
        RetrofitClient.apiService.unlinkStudent(parentId, student.id).enqueue(object : Callback<ApiResponse<String>> {
            override fun onResponse(
                call: Call<ApiResponse<String>>,
                response: Response<ApiResponse<String>>
            ) {
                hideLoading()
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        Snackbar.make(requireView(), "已解除与${student.username}的关联", Snackbar.LENGTH_SHORT).show()
                        // 重新加载学生列表
                        loadAssociatedStudents()
                    } else {
                        showError(apiResponse?.message ?: "解除关联失败")
                    }
                } else {
                    showError("解除关联失败: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<String>>, t: Throwable) {
                hideLoading()
                showError("网络错误: ${t.message}")
            }
        })
    }

    private fun showStudentsList(students: List<Student>) {
        adapter.updateStudents(students)
        recyclerView.visibility = View.VISIBLE
        noStudentsTextView.visibility = View.GONE
    }

    private fun showNoStudentsMessage() {
        recyclerView.visibility = View.GONE
        noStudentsTextView.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    inner class StudentAdapter(
        private val students: MutableList<Student>,
        private val onUnlinkClick: (Student) -> Unit
    ) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

        fun updateStudents(newStudents: List<Student>) {
            students.clear()
            students.addAll(newStudents)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_associated_student, parent, false)
            return StudentViewHolder(view)
        }

        override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
            val student = students[position]
            holder.bind(student)
        }

        override fun getItemCount(): Int = students.size

        inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val usernameTextView: TextView = itemView.findViewById(R.id.textView_student_username)
            private val unlinkButton: Button = itemView.findViewById(R.id.button_unlink)

            fun bind(student: Student) {
                usernameTextView.text = student.username
                unlinkButton.setOnClickListener {
                    onUnlinkClick(student)
                }
            }
        }
    }
}