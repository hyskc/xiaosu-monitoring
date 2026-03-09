package com.example.xiaosuserver.service;

import com.example.xiaosuserver.entity.Student;
import org.springframework.web.multipart.MultipartFile;

/**
 * 学生服务接口
 * 定义学生相关的业务逻辑操作
 */
public interface StudentService {
    
    /**
     * 学生登录
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回学生对象，失败返回null
     */
    Student login(String username, String password);
    
    /**
     * 学生注册
     * @param student 学生对象
     * @return 注册成功返回true，失败返回false
     */
    boolean register(Student student);
    
    /**
     * 根据用户名查找学生
     * @param username 用户名
     * @return 学生对象，如果不存在则返回null
     */
    Student findByUsername(String username);
    
    /**
     * 根据ID查找学生
     * @param id 学生ID
     * @return 学生对象，如果不存在则返回null
     */
    Student findById(Integer id);
    
    /**
     * 更新学生信息
     * @param student 学生对象
     * @return 更新后的学生对象
     */
    Student updateStudent(Student student);
    
    /**
     * 激活学生账号
     * @param username 用户名
     * @return 激活成功返回true，失败返回false
     */
    boolean activateStudent(String username);
    
    /**
     * 更新学生头像
     * @param studentId 学生ID
     * @param avatarFile 头像文件
     * @return 更新成功返回头像URL，失败返回null
     */
    String updateAvatar(Integer studentId, MultipartFile avatarFile);
}