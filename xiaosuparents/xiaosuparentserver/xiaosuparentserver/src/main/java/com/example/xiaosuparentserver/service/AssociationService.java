package com.example.xiaosuparentserver.service;

import com.example.xiaosuparentserver.dto.ApiResponse;
import com.example.xiaosuparentserver.dto.AssociationRequest;
import com.example.xiaosuparentserver.entity.Association;
import com.example.xiaosuparentserver.entity.Parent;
import com.example.xiaosuparentserver.entity.Student;
import com.example.xiaosuparentserver.repository.AssociationRepository;
import com.example.xiaosuparentserver.repository.ParentRepository;
import com.example.xiaosuparentserver.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AssociationService {

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AssociationRepository associationRepository;

    /**
     * 验证学生账号密码并关联到家长
     * @param request 关联请求
     * @return 关联结果
     */
    @Transactional
    public ApiResponse<String> associateStudentToParent(AssociationRequest request) {
        // 参数校验
        if (request.getParentId() == null || request.getStudentUsername() == null || 
                request.getStudentPassword() == null) {
            return ApiResponse.fail("参数不完整");
        }

        // 验证家长是否存在
        Parent parent = parentRepository.findById(request.getParentId());
        if (parent == null) {
            return ApiResponse.fail("家长不存在");
        }

        // 验证学生账号密码
        Student student = studentRepository.findByUsernameAndPassword(
                request.getStudentUsername(), request.getStudentPassword());
        if (student == null) {
            return ApiResponse.fail("学生账号或密码错误");
        }

        // 检查是否已经关联
        Association existingAssociation = associationRepository.findByParentIdAndStudentId(
                parent.getId(), student.getId());
        if (existingAssociation != null) {
            return ApiResponse.fail("已经关联过该学生");
        }

        // 创建关联
        Association association = new Association();
        association.setParentId(parent.getId());
        association.setStudentId(student.getId());
        associationRepository.save(association);

        return ApiResponse.success("关联成功", null);
    }
    
    /**
     * 获取家长关联的学生列表
     * @param parentId 家长ID
     * @return 学生列表
     */
    public ApiResponse<List<Student>> getAssociatedStudents(Integer parentId) {
        // 验证家长是否存在
        Parent parent = parentRepository.findById(parentId);
        if (parent == null) {
            return ApiResponse.fail("家长不存在");
        }
        
        // 获取关联记录
        List<Association> associations = associationRepository.findByParentId(parentId);
        if (associations.isEmpty()) {
            return ApiResponse.success(new ArrayList<>());
        }
        
        // 获取学生信息
        List<Student> students = new ArrayList<>();
        for (Association association : associations) {
            Student student = studentRepository.findById(association.getStudentId());
            if (student != null) {
                students.add(student);
            }
        }
        
        return ApiResponse.success(students);
    }
    
    /**
     * 解除家长与学生的关联
     * @param parentId 家长ID
     * @param studentId 学生ID
     * @return 解除关联结果
     */
    @Transactional
    public ApiResponse<String> unlinkStudent(Integer parentId, Integer studentId) {
        // 验证家长是否存在
        Parent parent = parentRepository.findById(parentId);
        if (parent == null) {
            return ApiResponse.fail("家长不存在");
        }
        
        // 验证学生是否存在
        Student student = studentRepository.findById(studentId);
        if (student == null) {
            return ApiResponse.fail("学生不存在");
        }
        
        // 验证关联关系是否存在
        Association association = associationRepository.findByParentIdAndStudentId(parentId, studentId);
        if (association == null) {
            return ApiResponse.fail("未找到关联关系");
        }
        
        // 解除关联
        int result = associationRepository.deleteByParentIdAndStudentId(parentId, studentId);
        if (result > 0) {
            return ApiResponse.success("解除关联成功", null);
        } else {
            return ApiResponse.fail("解除关联失败");
        }
    }
}