package com.example.xiaosuserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * 家长实体类
 * 对应数据库中的parents表
 */
@Entity
@Table(name = "parents")
public class Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "username", nullable = false, length = 50, unique = true)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Column(name = "password", nullable = false, length = 50)
    @NotBlank(message = "密码不能为空")
    private String password;
    
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    // 构造函数
    public Parent() {
    }

    public Parent(String username, String password, String code) {
        this.username = username;
        this.password = password;
        this.code = code;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}