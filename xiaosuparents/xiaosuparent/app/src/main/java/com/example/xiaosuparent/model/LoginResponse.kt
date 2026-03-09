package com.example.xiaosuparent.model

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val userId: Int?,
    val username: String?,
    val code: String?
)