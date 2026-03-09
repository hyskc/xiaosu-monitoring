package com.example.xiaosuparent.api

import com.example.xiaosuparent.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/parents/login")
    fun login(@Body loginRequest: LoginRequest): Call<ApiResponse<LoginResponse>>
    
    @POST("api/association/associate")
    fun associateStudent(@Body associationRequest: AssociationRequest): Call<Void>
    
    @GET("api/association/students/{parentId}")
    fun getAssociatedStudents(@Path("parentId") parentId: Int): Call<ApiResponse<List<Student>>>
    
    @DELETE("api/association/{parentId}/{studentId}")
    fun unlinkStudent(
        @Path("parentId") parentId: Int,
        @Path("studentId") studentId: Int
    ): Call<ApiResponse<String>>
    
    @GET("api/files/videos/{studentId}")
    fun getStudentVideos(@Path("studentId") studentId: Int): Call<ApiResponse<List<VideoFile>>>
    
    @GET("api/files/folder/{studentId}/{folderPath}")
    fun getFolderContents(
        @Path("studentId") studentId: Int,
        @Path(value = "folderPath", encoded = true) folderPath: String
    ): Call<ApiResponse<List<VideoFile>>>
    
    // 获取学生应用使用情况
    @GET("api/monitor/app-usage/{studentId}")
    fun getAppUsageInfo(
        @Path("studentId") studentId: Int,
        @Query("timeRange") timeRange: String // 可选值: today, week, month, custom
    ): Call<ApiResponse<List<AppUsageInfo>>>
}