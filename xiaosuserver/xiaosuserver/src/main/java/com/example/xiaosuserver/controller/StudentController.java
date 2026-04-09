package com.example.xiaosuserver.controller;

import com.example.xiaosuserver.entity.Student;
import com.example.xiaosuserver.model.ApiResponse;
import com.example.xiaosuserver.model.LoginRequest;
import com.example.xiaosuserver.model.RegisterRequest;
import com.example.xiaosuserver.model.StudentResponse;
import com.example.xiaosuserver.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 学生控制器
 * 处理学生相关的HTTP请求
 */
@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*") // 允许所有来源的跨域请求
public class StudentController {

    private final StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /**
     * 学生登录
     * @param loginRequest 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<StudentResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        Student student = studentService.login(loginRequest.getUsername(), loginRequest.getPassword());
        
        if (student == null) {
            return ResponseEntity.ok(ApiResponse.fail("用户名或密码错误"));
        }
        
        if (!student.isActive()) {
            return ResponseEntity.ok(ApiResponse.fail("账号未激活"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("登录成功", StudentResponse.fromEntity(student)));
    }

    /**
     * 学生注册
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        Student student = new Student(registerRequest.getUsername(), registerRequest.getPassword());
        
        boolean success = studentService.register(student);
        
        if (!success) {
            return ResponseEntity.ok(ApiResponse.fail("用户名已存在"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("注册成功，等待管理员激活", null));
    }

    /**
     * 获取学生信息
     * @param username 用户名
     * @return 学生信息
     */
    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudentInfo(@PathVariable String username) {
        Student student = studentService.findByUsername(username);
        
        if (student == null) {
            return ResponseEntity.ok(ApiResponse.fail("学生不存在"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(StudentResponse.fromEntity(student)));
    }

    /**
     * 激活学生账号（管理员操作）
     * @param username 用户名
     * @return 激活结果
     */
    @PostMapping("/activate/{username}")
    public ResponseEntity<ApiResponse<Void>> activateStudent(@PathVariable String username) {
        boolean success = studentService.activateStudent(username);
        
        if (!success) {
            return ResponseEntity.ok(ApiResponse.fail("学生不存在"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("账号激活成功", null));
    }
    
    /**
     * 更新学生头像
     * @param studentId 学生ID
     * @param avatarFile 头像文件
     * @return 更新结果
     */
    @PostMapping("/avatar/{studentId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateAvatar(
            @PathVariable Integer studentId,
            @RequestParam("avatar") MultipartFile avatarFile) {
        
        if (avatarFile.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("请选择头像文件"));
        }
        
        String avatarPath = studentService.updateAvatar(studentId, avatarFile);
        
        if (avatarPath == null) {
            return ResponseEntity.ok(ApiResponse.fail("更新头像失败，学生不存在或文件保存出错"));
        }
        
        Map<String, String> data = new HashMap<>();
        data.put("avatarPath", avatarPath);
        
        return ResponseEntity.ok(ApiResponse.success("头像更新成功", data));
    }
    
    /**
     * 获取学生头像
     * @param studentId 学生ID
     * @return 头像URL
     */
    @GetMapping("/avatar/{studentId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAvatar(@PathVariable Integer studentId) {
        // 检查学生是否存在
        Student student = studentService.findById(studentId);
        
        if (student == null) {
            return ResponseEntity.ok(ApiResponse.fail("学生不存在"));
        }
        
        // 构建头像文件夹路径，使用相对路径确保跨平台兼容性
        String baseDir = System.getProperty("user.dir") + File.separator + "file";
        String avatarFolderPath = baseDir + File.separator + studentId + File.separator + "avatar";
        File avatarFolder = new File(avatarFolderPath);
        
        if (!avatarFolder.exists() || !avatarFolder.isDirectory()) {
            return ResponseEntity.ok(ApiResponse.fail("头像不存在"));
        }
        
        // 查找固定名称的头像文件
        File avatarFile = new File(avatarFolderPath + File.separator + "avatar.jpg");
        if (!avatarFile.exists()) {
            avatarFile = new File(avatarFolderPath + File.separator + "avatar.png");
        }
        if (!avatarFile.exists()) {
            avatarFile = new File(avatarFolderPath + File.separator + "avatar.jpeg");
        }
        if (!avatarFile.exists()) {
            // 尝试查找任何以avatar开头的文件
            File[] avatarFiles = avatarFolder.listFiles((dir, name) -> name.startsWith("avatar"));
            if (avatarFiles == null || avatarFiles.length == 0) {
                return ResponseEntity.ok(ApiResponse.fail("头像不存在"));
            }
            avatarFile = avatarFiles[0];
        }
        
        // 构建头像URL
        String avatarPath = studentId + "/avatar/" + avatarFile.getName();
        
        Map<String, String> data = new HashMap<>();
        data.put("avatarPath", avatarPath);
        
        return ResponseEntity.ok(ApiResponse.success("获取头像成功", data));
    }
}