package com.example.xiaosuparent.model

import com.google.gson.annotations.SerializedName

data class VideoFile(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val fileType: String,
    val lastModified: String,
    @SerializedName("directory")
    val isDirectory: Boolean = false,
    val streamUrl: String? = null // 视频流式传输URL，支持进度条跳转功能
)