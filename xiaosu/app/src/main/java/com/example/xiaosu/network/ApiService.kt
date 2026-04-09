package com.example.xiaosu.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import com.example.xiaosu.config.AppConfig

interface ApiService {
    /**
     * 学生登录
     * @param username 用户名
     * @param password 密码
     * @return 登录响应
     */
    @POST(AppConfig.API_LOGIN_PATH)
    fun login(
        @Body loginRequest: LoginRequest
    ): Call<ApiResponse<StudentResponse>>

    /**
     * 学生注册
     * @param username 用户名
     * @param password 密码
     * @return 注册响应
     */
    @POST(AppConfig.API_REGISTER_PATH)
    fun register(
        @Body registerRequest: RegisterRequest
    ): Call<ApiResponse<Void>>

    /**
     * 获取学生信息
     * @param username 用户名
     * @return 学生信息
     */
    @GET("api/students/{username}")
    fun getStudentInfo(
        @Path("username") username: String
    ): Call<ApiResponse<StudentResponse>>

    /**
     * 检查文件上传状态
     * @param fileName 文件名
     * @param fileSize 文件总大小
     * @return 已上传的字节数
     */
    @GET("${AppConfig.API_UPLOAD_PATH}/status")
    fun checkUploadStatus(
        @Query("fileName") fileName: String,
        @Query("fileSize") fileSize: Long
    ): Call<UploadStatusResponse>

    /**
     * 上传文件块
     * @param fileName 文件名
     * @param chunkIndex 块索引
     * @param chunkTotal 总块数
     * @param startByte 开始字节位置
     * @param filePart 文件块数据
     * @return 上传响应
     */
    @Multipart
    @POST("${AppConfig.API_UPLOAD_PATH}/chunk")
    fun uploadChunk(
        @Part("fileName") fileName: String,
        @Part("chunkIndex") chunkIndex: Int,
        @Part("chunkTotal") chunkTotal: Int,
        @Part("startByte") startByte: Long,
        @Part filePart: MultipartBody.Part
    ): Call<UploadResponse>

    /**
     * 完成文件上传
     * @param fileName 文件名
     * @param fileSize 文件总大小
     * @return 完成响应
     */
    @POST("${AppConfig.API_UPLOAD_PATH}/complete")
    fun completeUpload(
        @Query("fileName") fileName: String,
        @Query("fileSize") fileSize: Long
    ): Call<UploadCompleteResponse>

    /**
     * 上传学生头像
     * @param studentId 学生ID
     * @param file 头像文件
     * @return 上传响应
     */
    @Multipart
    @POST("${AppConfig.API_AVATAR_PATH}/{studentId}")
    fun uploadAvatar(
        @Path("studentId") studentId: Int,
        @Part avatar: MultipartBody.Part
    ): Call<ApiResponse<Map<String, String>>>

    /**
     * 获取学生头像URL
     * @param studentId 学生ID
     * @return 头像URL响应
     */
    @GET("${AppConfig.API_AVATAR_PATH}/{studentId}")
    fun getAvatarUrl(
        @Path("studentId") studentId: Int
    ): Call<ApiResponse<Map<String, String>>>
    
    /**
     * 获取学生关联的家长账号信息
     * @param studentId 学生ID
     * @return 关联的家长账号信息
     */
    @GET("${AppConfig.API_ASSOCIATION_PATH}/student/{studentId}")
    fun getAssociatedParents(
        @Path("studentId") studentId: Int
    ): Call<ApiResponse<List<ParentResponse>>>
    
    /**
     * 验证家长code
     * @param code 家长code
     * @return 验证结果
     */
    @POST("${AppConfig.API_VALIDATE_PARENT_CODE_PATH}")
    fun validateParentCode(
        @Query("code") code: String
    ): Call<ApiResponse<Map<String, Any>>>
}

// 登录请求模型
data class LoginRequest(
    val username: String,
    val password: String
)

// 注册请求模型
data class RegisterRequest(
    val username: String,
    val password: String
)

// 通用API响应模型
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)

// 学生响应模型
data class StudentResponse(
    val id: Int,
    val username: String,
    val active: Boolean
)

// 家长响应模型
data class ParentResponse(
    val id: Int,
    val username: String,
    val code: String
)

data class UploadStatusResponse(
    val uploaded: Long,  // 已上传的字节数
    val status: String   // 状态："not_found" 或 "in_progress"
)

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val bytesUploaded: Long
)

data class UploadCompleteResponse(
    val success: Boolean,
    val message: String,
    val filePath: String
)