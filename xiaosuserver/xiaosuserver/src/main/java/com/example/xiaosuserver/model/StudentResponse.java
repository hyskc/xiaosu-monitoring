package com.example.xiaosuserver.model;

import com.example.xiaosuserver.entity.Student;

/**
 * 学生信息响应模型
 * 用于返回给前端的学生信息，不包含敏感信息
 */
public class StudentResponse {

    private Integer id;
    private String username;
    private boolean active;

    // 构造函数
    public StudentResponse() {
    }

    public StudentResponse(Integer id, String username, boolean active) {
        this.id = id;
        this.username = username;
        this.active = active;
    }

    // 从实体转换为响应模型的静态工厂方法
    public static StudentResponse fromEntity(Student student) {
        if (student == null) {
            return null;
        }
        return new StudentResponse(student.getId(), student.getUsername(), student.isActive());
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}