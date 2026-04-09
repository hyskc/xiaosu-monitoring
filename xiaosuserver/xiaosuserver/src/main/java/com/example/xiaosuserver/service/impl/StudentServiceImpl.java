package com.example.xiaosuserver.service.impl;

import com.example.xiaosuserver.entity.Student;
import com.example.xiaosuserver.repository.StudentRepository;
import com.example.xiaosuserver.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 学生服务实现类
 */
@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    @Autowired
    public StudentServiceImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public Student login(String username, String password) {
        return studentRepository.findByUsernameAndPassword(username, password);
    }

    @Override
    public boolean register(Student student) {
        // 检查用户名是否已存在
        if (studentRepository.existsByUsername(student.getUsername())) {
            return false;
        }

        // 设置默认状态为未激活
        student.setActive(false);

        // 保存学生信息并获取保存后的对象（包含生成的ID）
        Student savedStudent = studentRepository.save(student);

        // 创建用户文件夹
        createUserFolders(savedStudent);

        return true;
    }

    /**
     * 创建用户相关的文件夹
     * @param student 学生对象
     */
    private void createUserFolders(Student student) {
        try {
            // 文件存储的相对路径，使用File.separator确保跨平台兼容性
            String baseDir = System.getProperty("user.dir") + File.separator + "file";

            // 创建用户文件夹（只使用id）
            String userFolderName = student.getId().toString();
            File userFolder = new File(baseDir, userFolderName);
            if (!userFolder.exists()) {
                userFolder.mkdirs();
            }

            // 创建视频文件夹
            File videoFolder = new File(userFolder, "video");
            if (!videoFolder.exists()) {
                videoFolder.mkdirs();
            }

            // 创建头像文件夹
            File avatarFolder = new File(userFolder, "avatar");
            if (!avatarFolder.exists()) {
                avatarFolder.mkdirs();
            }
        } catch (Exception e) {
            // 记录异常，但不影响注册流程
            System.err.println("创建用户文件夹失败: " + e.getMessage());
        }
    }

    @Override
    public Student findByUsername(String username) {
        return studentRepository.findByUsername(username);
    }

    @Override
    public Student findById(Integer id) {
        return studentRepository.findById(id).orElse(null);
    }

    @Override
    public Student updateStudent(Student student) {
        // 检查学生是否存在
        if (!studentRepository.existsByUsername(student.getUsername())) {
            return null;
        }

        // 更新学生信息
        return studentRepository.save(student);
    }

    @Override
    public boolean activateStudent(String username) {
        // 查找学生
        Student student = studentRepository.findByUsername(username);
        if (student == null) {
            return false;
        }

        // 激活账号
        student.setActive(true);
        studentRepository.save(student);
        return true;
    }

    @Override
    public String updateAvatar(Integer studentId, MultipartFile avatarFile) {
        try {
            // 检查学生是否存在
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                return null;
            }

            // 文件存储的相对路径，确保跨平台兼容性
            String baseDir = System.getProperty("user.dir") + File.separator + "file";

            // 用户文件夹路径
            String userFolderPath = baseDir + File.separator + studentId;
            File userFolder = new File(userFolderPath);
            if (!userFolder.exists()) {
                userFolder.mkdirs();
            }

            // 头像文件夹路径
            String avatarFolderPath = userFolderPath + File.separator + "avatar";
            File avatarFolder = new File(avatarFolderPath);
            if (!avatarFolder.exists()) {
                avatarFolder.mkdirs();
            }

            // 删除avatar文件夹中的所有文件
            File[] oldAvatarFiles = avatarFolder.listFiles();
            if (oldAvatarFiles != null) {
                for (File oldFile : oldAvatarFiles) {
                    if (oldFile.isFile()) {
                        oldFile.delete();
                    }
                }
            }

            // 使用固定的文件名，便于二次登录时查找
            String originalFilename = avatarFile.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFileName = "avatar" + fileExtension;

            // 保存文件
            Path targetPath = Paths.get(avatarFolderPath, newFileName);
            Files.copy(avatarFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 返回头像URL（相对路径）
            return studentId + "/avatar/" + newFileName;

        } catch (IOException e) {
            System.err.println("更新头像失败: " + e.getMessage());
            return null;
        }
    }
}
