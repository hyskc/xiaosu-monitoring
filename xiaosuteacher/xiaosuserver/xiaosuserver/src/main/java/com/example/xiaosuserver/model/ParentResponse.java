package com.example.xiaosuserver.model;

import com.example.xiaosuserver.entity.Parent;

/**
 * 家长响应模型
 * 用于向客户端返回家长信息
 */
public class ParentResponse {

    private Integer id;
    private String username;
    private String code;

    // 构造函数
    public ParentResponse() {
    }

    public ParentResponse(Integer id, String username, String code) {
        this.id = id;
        this.username = username;
        this.code = code;
    }

    // 从实体转换为响应模型的静态方法
    public static ParentResponse fromEntity(Parent parent) {
        if (parent == null) {
            return null;
        }
        return new ParentResponse(parent.getId(), parent.getUsername(), parent.getCode());
    }

    // Getter和Setter方法
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}