package com.example.xiaosuparent.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xiaosuparent.R
import com.example.xiaosuparent.adapters.VideoAdapter
import com.example.xiaosuparent.api.RetrofitClient
import com.example.xiaosuparent.model.ApiResponse
import com.example.xiaosuparent.model.Student
import com.example.xiaosuparent.model.VideoFile
import com.example.xiaosuparent.utils.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private lateinit var userSession: UserSession
    private lateinit var studentSpinner: Spinner
    private lateinit var recyclerViewFiles: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var currentPathTextView: TextView
    private lateinit var buttonBack: Button
    
    private var studentList = mutableListOf<Student>()
    private var fileAdapter: VideoAdapter? = null
    private var currentStudentId: Int = -1
    private var currentPath: String = ""
    private var pathStack = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化用户会话
        userSession = UserSession.getInstance(requireContext())

        // 初始化视图
        studentSpinner = view.findViewById(R.id.spinner_students)
        recyclerViewFiles = view.findViewById(R.id.recyclerView_videos)
        emptyTextView = view.findViewById(R.id.textView_empty)
        currentPathTextView = view.findViewById(R.id.textView_currentPath)
        buttonBack = view.findViewById(R.id.button_back)

        // 设置RecyclerView
        recyclerViewFiles.layoutManager = LinearLayoutManager(requireContext())
        fileAdapter = VideoAdapter { file -> 
            handleFileClick(file)
        }
        recyclerViewFiles.adapter = fileAdapter

        // 设置返回按钮点击事件
        buttonBack.setOnClickListener {
            navigateBack()
        }
        
        // 初始状态下隐藏返回按钮和路径显示
        buttonBack.visibility = View.GONE
        currentPathTextView.visibility = View.GONE

        // 检查用户是否已登录
        if (userSession.isLoggedIn()) {
            // 获取关联的学生列表
            loadAssociatedStudents()
        } else {
            // 显示未登录提示
            emptyTextView.visibility = View.VISIBLE
            emptyTextView.text = "请先登录以查看文件"
            studentSpinner.visibility = View.GONE
            recyclerViewFiles.visibility = View.GONE
        }

        // 设置学生选择监听器
        studentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (studentList.isNotEmpty() && position < studentList.size) {
                    val selectedStudent = studentList[position]
                    // 重置路径状态
                    resetPathState()
                    // 加载学生根目录文件
                    currentStudentId = selectedStudent.id
                    loadStudentRootFiles(currentStudentId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 不做任何操作
            }
        }
    }
    
    private fun resetPathState() {
        currentPath = ""
        pathStack.clear()
        buttonBack.visibility = View.GONE
        currentPathTextView.visibility = View.GONE
        currentPathTextView.text = "/"
    }

    private fun loadAssociatedStudents() {
        val parentId = userSession.getUserId()
        if (parentId <= 0) {
            showError("无法获取用户ID")
            return
        }

        val apiService = RetrofitClient.apiService
        apiService.getAssociatedStudents(parentId).enqueue(object : Callback<ApiResponse<List<Student>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<Student>>>,
                response: Response<ApiResponse<List<Student>>>
            ) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        val students = apiResponse.data
                        if (students != null && students.isNotEmpty()) {
                            studentList.clear()
                            studentList.addAll(students)
                            setupStudentSpinner()
                            // 默认加载第一个学生的文件
                            if (studentList.isNotEmpty()) {
                                currentStudentId = studentList[0].id
                                loadStudentRootFiles(currentStudentId)
                            }
                        } else {
                            showEmptyStudents()
                        }
                    } else {
                        showError(apiResponse?.message ?: "获取学生列表失败")
                    }
                } else {
                    showError("服务器响应错误: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Student>>>, t: Throwable) {
                showError("网络错误: ${t.message}")
            }
        })
    }

    private fun setupStudentSpinner() {
        if (studentList.isEmpty()) {
            studentSpinner.visibility = View.GONE
            return
        }

        studentSpinner.visibility = View.VISIBLE
        val studentNames = studentList.map { it.username }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, studentNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        studentSpinner.adapter = adapter
    }

    private fun loadStudentRootFiles(studentId: Int) {
        // 加载学生根目录的文件和文件夹
        val apiService = RetrofitClient.apiService
        apiService.getStudentVideos(studentId).enqueue(object : Callback<ApiResponse<List<VideoFile>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<VideoFile>>>,
                response: Response<ApiResponse<List<VideoFile>>>
            ) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        val files = apiResponse.data
                        if (files != null && files.isNotEmpty()) {
                            showFiles(files)
                        } else {
                            showEmptyFiles()
                        }
                    } else {
                        showError(apiResponse?.message ?: "获取文件列表失败")
                    }
                } else {
                    showError("服务器响应错误: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<VideoFile>>>, t: Throwable) {
                showError("网络错误: ${t.message}")
            }
        })
    }
    
    private fun loadFolderContents(studentId: Int, folderPath: String) {
        val apiService = RetrofitClient.apiService
        apiService.getFolderContents(studentId, folderPath).enqueue(object : Callback<ApiResponse<List<VideoFile>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<VideoFile>>>,
                response: Response<ApiResponse<List<VideoFile>>>
            ) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        val files = apiResponse.data
                        if (files != null && files.isNotEmpty()) {
                            showFiles(files)
                        } else {
                            showEmptyFiles()
                        }
                    } else {
                        showError(apiResponse?.message ?: "获取文件夹内容失败")
                    }
                } else {
                    showError("服务器响应错误: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<VideoFile>>>, t: Throwable) {
                showError("网络错误: ${t.message}")
            }
        })
    }

    private fun showFiles(files: List<VideoFile>) {
        emptyTextView.visibility = View.GONE
        recyclerViewFiles.visibility = View.VISIBLE
        fileAdapter?.updateFiles(files)
    }

    private fun showEmptyFiles() {
        emptyTextView.visibility = View.VISIBLE
        emptyTextView.text = "没有可用的文件"
        recyclerViewFiles.visibility = View.GONE
    }

    private fun showEmptyStudents() {
        studentSpinner.visibility = View.GONE
        emptyTextView.visibility = View.VISIBLE
        emptyTextView.text = "没有关联的学生，请先关联学生"
        recyclerViewFiles.visibility = View.GONE
    }

    private fun showError(message: String) {
        emptyTextView.visibility = View.VISIBLE
        emptyTextView.text = message
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    private fun handleFileClick(file: VideoFile) {
        if (file.isDirectory) {
            // 处理文件夹点击
            navigateToFolder(file)
        } else {
            // 处理文件点击
            openFile(file)
        }
    }
    
    private fun navigateToFolder(folder: VideoFile) {
        // 保存当前路径到栈中
        pathStack.add(currentPath)
        
        // 更新当前路径
        val folderName = folder.fileName
        currentPath = if (currentPath.isEmpty()) folderName else "$currentPath/$folderName"
        
        // 更新UI
        currentPathTextView.text = "/$currentPath"
        currentPathTextView.visibility = View.VISIBLE
        buttonBack.visibility = View.VISIBLE
        
        // 加载文件夹内容
        loadFolderContents(currentStudentId, currentPath)
    }
    
    private fun navigateBack() {
        if (pathStack.isNotEmpty()) {
            // 从栈中取出上一级路径
            currentPath = pathStack.removeAt(pathStack.size - 1)
            
            // 更新UI
            if (currentPath.isEmpty()) {
                currentPathTextView.visibility = View.GONE
                buttonBack.visibility = View.GONE
                loadStudentRootFiles(currentStudentId)
            } else {
                currentPathTextView.text = "/$currentPath"
                loadFolderContents(currentStudentId, currentPath)
            }
        } else {
            // 已经在根目录，重新加载根目录文件
            resetPathState()
            loadStudentRootFiles(currentStudentId)
        }
    }

    private fun openFile(file: VideoFile) {
        var baseUrl = RetrofitClient.BASE_URL
        
        // 确保baseUrl以/结尾
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/"
        }
        
        try {
            // 判断文件类型
            val isVideo = file.fileType.startsWith("video/") || 
                    file.fileName.endsWith(".mp4", ignoreCase = true) || 
                    file.fileName.endsWith(".avi", ignoreCase = true) || 
                    file.fileName.endsWith(".mov", ignoreCase = true)
            
            if (isVideo) {
                // 优先使用流式传输URL（如果存在），否则使用普通下载URL
                var videoUrl = if (file.streamUrl != null && file.streamUrl.isNotEmpty()) {
                    // 确保streamUrl不以/开头，避免路径重复
                    val streamPath = if (file.streamUrl.startsWith("/")) file.streamUrl.substring(1) else file.streamUrl
                    baseUrl + streamPath
                } else {
                    // 确保filePath不以/开头，避免路径重复
                    val filePath = if (file.filePath.startsWith("/")) file.filePath.substring(1) else file.filePath
                    baseUrl + filePath
                }
                
                // 记录视频URL到日志，便于调试
                android.util.Log.d("HomeFragment", "视频URL: $videoUrl")
                
                // 使用ExoPlayer播放器打开视频文件，提供更好的进度条跳转功能
                val intent = Intent(requireContext(), com.example.xiaosuparent.activities.ExoPlayerActivity::class.java)
                intent.putExtra(com.example.xiaosuparent.activities.ExoPlayerActivity.EXTRA_VIDEO_URL, videoUrl)
                intent.putExtra(com.example.xiaosuparent.activities.ExoPlayerActivity.EXTRA_VIDEO_TITLE, file.fileName)
                startActivity(intent)
            } else {
                // 其他类型文件使用系统默认应用打开
                // 确保filePath不以/开头，避免路径重复
                val filePath = if (file.filePath.startsWith("/")) file.filePath.substring(1) else file.filePath
                val fileUrl = baseUrl + filePath
                
                // 记录文件URL到日志，便于调试
                android.util.Log.d("HomeFragment", "文件URL: $fileUrl")
                
                val intent = Intent(Intent.ACTION_VIEW)
                val mimeType = when {
                    file.fileType.isNotEmpty() -> file.fileType
                    file.fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                    file.fileName.endsWith(".doc", ignoreCase = true) || file.fileName.endsWith(".docx", ignoreCase = true) -> "application/msword"
                    file.fileName.endsWith(".xls", ignoreCase = true) || file.fileName.endsWith(".xlsx", ignoreCase = true) -> "application/vnd.ms-excel"
                    file.fileName.endsWith(".ppt", ignoreCase = true) || file.fileName.endsWith(".pptx", ignoreCase = true) -> "application/vnd.ms-powerpoint"
                    file.fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
                    file.fileName.endsWith(".jpg", ignoreCase = true) || file.fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    file.fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                    else -> "*/*"
                }
                intent.setDataAndType(Uri.parse(fileUrl), mimeType)
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "没有找到可以打开此类型文件的应用", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("HomeFragment", "无法找到打开文件的应用: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "无法打开文件: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("HomeFragment", "打开文件失败: ${e.message}", e)
        }
    }
}