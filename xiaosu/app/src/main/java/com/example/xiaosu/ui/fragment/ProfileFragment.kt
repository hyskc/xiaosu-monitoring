package com.example.xiaosu.ui.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.xiaosu.R
import com.example.xiaosu.config.AppConfig
import com.example.xiaosu.network.ApiClient
import com.example.xiaosu.network.ApiResponse
import com.example.xiaosu.network.LoginRequest
import com.example.xiaosu.network.StudentResponse
import com.example.xiaosu.service.ScreenRecordService
import com.example.xiaosu.ui.login.LoginActivity
import de.hdodenhof.circleimageview.CircleImageView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private lateinit var profileImageView: CircleImageView
    private lateinit var usernameTextView: TextView
    private lateinit var accountSettingsTextView: TextView
    private lateinit var notificationSettingsTextView: TextView
    private lateinit var appSettingsTextView: TextView
    private lateinit var aboutTextView: TextView
    private lateinit var loginContainer: LinearLayout
    private lateinit var settingsContainer: LinearLayout
    private lateinit var goToLoginButton: Button
    private lateinit var userInfoCard: CardView
    
    private var isLoggedIn = false
    private var username = ""
    private var userId = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        
        // 初始化视图
        profileImageView = view.findViewById(R.id.profileImageView)
        usernameTextView = view.findViewById(R.id.usernameTextView)
        accountSettingsTextView = view.findViewById(R.id.accountSettingsTextView)
        notificationSettingsTextView = view.findViewById(R.id.notificationSettingsTextView)
        appSettingsTextView = view.findViewById(R.id.appSettingsTextView)
        aboutTextView = view.findViewById(R.id.aboutTextView)
        loginContainer = view.findViewById(R.id.loginContainer)
        settingsContainer = view.findViewById(R.id.settingsContainer)
        userInfoCard = view.findViewById(R.id.userInfoCard)
        goToLoginButton = view.findViewById(R.id.goToLoginButton)
        
        // 检查登录状态
        checkLoginState()
        
        setupListeners()
        
        return view
    }
    
    override fun onResume() {
        super.onResume()
        // 每次页面可见时检查登录状态
        checkLoginState()
    }
    
    private fun checkLoginState() {
        val sharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)
        userId = sharedPreferences.getInt("user_id", 0)
        username = sharedPreferences.getString("username", "") ?: ""
        
        // 根据登录状态更新UI
        updateUI()
    }
    
    private fun updateUI() {
        if (isLoggedIn) {
            // 已登录状态
            usernameTextView.text = username
            settingsContainer.visibility = View.VISIBLE
            loginContainer.visibility = View.GONE
            // 加载用户头像（如果有）
            loadUserAvatar()
        } else {
            // 未登录状态
            usernameTextView.text = "未登录"
            settingsContainer.visibility = View.GONE
            loginContainer.visibility = View.VISIBLE
            // 恢复默认头像
            profileImageView.setImageResource(R.drawable.default_avatar)
        }
    }
    
    // 加载用户头像的方法
    private fun loadUserAvatar() {
        val sharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        val studentId = sharedPreferences.getInt("user_id", -1)
        
        if (studentId != -1) {
            // 用户已登录，尝试获取头像
            val apiService = ApiClient.apiService
            apiService.getAvatarUrl(studentId).enqueue(object : Callback<ApiResponse<Map<String, String>>> {
                override fun onResponse(
                    call: Call<ApiResponse<Map<String, String>>>,
                    response: Response<ApiResponse<Map<String, String>>>
                ) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.success && apiResponse.data != null) {
                            // 获取到头像URL，加载头像
                            val avatarPath = apiResponse.data["avatarPath"]
                            if (avatarPath != null) {
                                // 添加时间戳参数，确保每次都加载最新的头像
                                val timestamp = System.currentTimeMillis()
                                val avatarUrl = "${AppConfig.SERVER_BASE_URL}${avatarPath}?t=$timestamp"
                                if (isAdded && context != null) {
                                    Glide.with(requireContext())
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.default_avatar)
                                        .error(R.drawable.default_avatar)
                                        .into(profileImageView)
                                }
                            } else if (isAdded && context != null) {
                                // 使用默认头像
                                profileImageView.setImageResource(R.drawable.default_avatar)
                            }
                        } else if (isAdded && context != null) {
                            // 使用默认头像
                            profileImageView.setImageResource(R.drawable.default_avatar)
                        }
                    } else if (isAdded && context != null) {
                        // 使用默认头像
                        profileImageView.setImageResource(R.drawable.default_avatar)
                    }
                }
                
                override fun onFailure(call: Call<ApiResponse<Map<String, String>>>, t: Throwable) {
                    // 网络请求失败，使用默认头像
                    if (isAdded && context != null) {
                        profileImageView.setImageResource(R.drawable.default_avatar)
                    }
                }
            })
        } else {
            // 用户未登录，使用默认头像
            profileImageView.setImageResource(R.drawable.default_avatar)
        }
    }

    private fun setupListeners() {
        // 跳转到新的登录界面
        goToLoginButton.setOnClickListener {
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            startActivity(intent)
        }
        
        accountSettingsTextView.setOnClickListener {
            // 跳转到账号设置页面
            val sharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
            val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)
            
            if (isLoggedIn) {
                // 跳转到账号设置页面
                val intent = Intent(requireActivity(), com.example.xiaosu.ui.settings.AccountSettingsActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            }
        }
        
        notificationSettingsTextView.setOnClickListener {
            // TODO: 跳转到通知设置页面
            Toast.makeText(context, "通知设置功能待实现", Toast.LENGTH_SHORT).show()
        }
        
        appSettingsTextView.setOnClickListener {
            // 跳转到应用设置页面
            val intent = Intent(requireActivity(), com.example.xiaosu.ui.settings.SettingsActivity::class.java)
            startActivity(intent)
        }
        
        aboutTextView.setOnClickListener {
            // 跳转到关于我们页面
            val intent = Intent(requireActivity(), com.example.xiaosu.ui.about.AboutUsActivity::class.java)
            startActivity(intent)
        }
        
        // 头像点击事件
        profileImageView.setOnClickListener {
            if (isLoggedIn) {
                // 用户已登录，打开图片选择器
                openImagePicker()
            } else {
                Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private val PICK_IMAGE_REQUEST = 1
    private val PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2
    
    private fun openImagePicker() {
        // 检查权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val permission = android.Manifest.permission.READ_EXTERNAL_STORAGE
            if (requireActivity().checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), PERMISSION_REQUEST_READ_EXTERNAL_STORAGE)
                return
            }
        }
        
        // 打开图片选择器
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
    
    private fun logout() {
        // 清除登录状态和用户信息
        val sharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
        
        // 停止屏幕录制服务
        stopScreenRecordService()
        
        // 更新UI
        isLoggedIn = false
        username = ""
        userId = 0
        updateUI()
        
        Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopScreenRecordService() {
        val serviceIntent = Intent(requireContext(), ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        requireContext().startService(serviceIntent)
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // 权限获取成功，打开图片选择器
                openImagePicker()
            } else {
                // 权限被拒绝
                Toast.makeText(requireContext(), "需要存储权限才能选择头像", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == android.app.Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data
            // 显示选中的图片
            Glide.with(requireContext())
                .load(imageUri)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .into(profileImageView)
            
            // 上传头像
            if (isAdded && context != null) {
                uploadAvatar(imageUri)
            }
        }
    }
    
    private fun uploadAvatar(imageUri: android.net.Uri?) {
        if (imageUri == null) return
        
        try {
            // 检查Fragment是否附加到Context
            if (!isAdded || context == null) {
                return
            }
            
            // 获取学生ID
            val sharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
            val studentId = sharedPreferences.getInt("user_id", -1)
            if (studentId == -1) {
                Toast.makeText(requireContext(), "用户未登录", Toast.LENGTH_SHORT).show()
                return
            }
            
            // 获取文件路径
            val filePathColumn = arrayOf(android.provider.MediaStore.Images.Media.DATA)
            val cursor = requireActivity().contentResolver.query(imageUri, filePathColumn, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(filePathColumn[0])
                    val filePath = it.getString(columnIndex)
                    val file = java.io.File(filePath)
                    
                    // 创建MultipartBody.Part
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("avatar", file.name, requestFile)
                    
                    // 调用API上传头像
                    val apiService = ApiClient.apiService
                    apiService.uploadAvatar(studentId, body).enqueue(object : Callback<ApiResponse<Map<String, String>>> {
                        override fun onResponse(
                            call: Call<ApiResponse<Map<String, String>>>,
                            response: Response<ApiResponse<Map<String, String>>>
                        ) {
                            if (response.isSuccessful) {
                                val apiResponse = response.body()
                                if (apiResponse != null && apiResponse.success) {
                                    if (isAdded && context != null) {
                                        Toast.makeText(requireContext(), "头像上传成功", Toast.LENGTH_SHORT).show()
                                        // 上传成功后重新加载头像
                                        loadUserAvatar()
                                    }
                                } else if (isAdded && context != null) {
                                    Toast.makeText(requireContext(), "头像上传失败: ${apiResponse?.message ?: "未知错误"}", Toast.LENGTH_SHORT).show()
                                }
                            } else if (isAdded && context != null) {
                                Toast.makeText(requireContext(), "头像上传失败: ${response.message()}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        
                        override fun onFailure(call: Call<ApiResponse<Map<String, String>>>, t: Throwable) {
                            if (isAdded && context != null) {
                                Toast.makeText(requireContext(), "头像上传失败: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
            }
        } catch (e: Exception) {
            if (isAdded && context != null) {
                Toast.makeText(requireContext(), "头像上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 登录方法已移至LoginActivity
    
    private fun saveLoginState(studentResponse: StudentResponse?) {
        if (studentResponse == null) return
        
        // 使用SharedPreferences保存登录状态和用户信息
        val sharedPreferences = requireActivity().getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("is_logged_in", true)
        editor.putInt("user_id", studentResponse.id)
        editor.putString("username", studentResponse.username)
        editor.putBoolean("is_active", studentResponse.active)
        editor.apply()
    }

    // 注册方法已移至LoginActivity
    
    companion object {
        fun newInstance() = ProfileFragment()
    }
}