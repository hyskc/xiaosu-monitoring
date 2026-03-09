package com.example.xiaosuparent.model

data class AssociationRequest(
    val parentId: Int,
    val studentUsername: String,
    val studentPassword: String
)