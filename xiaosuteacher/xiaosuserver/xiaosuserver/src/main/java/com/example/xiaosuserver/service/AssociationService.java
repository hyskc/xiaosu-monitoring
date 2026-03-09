package com.example.xiaosuserver.service;

import com.example.xiaosuserver.entity.Association;
import com.example.xiaosuserver.entity.Parent;
import com.example.xiaosuserver.entity.Student;
import com.example.xiaosuserver.repository.AssociationRepository;
import com.example.xiaosuserver.repository.StudentRepository;
import com.example.xiaosuserver.model.ParentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 关联关系服务类
 */
@Service
public class AssociationService {

    @Autowired
    private AssociationRepository associationRepository;
    
    @Autowired
    private StudentRepository studentRepository;

    /**
     * 根据学生ID获取关联的家长列表
     *
     * @param studentId 学生ID
     * @return 家长响应列表
     */
    public List<ParentResponse> getAssociatedParents(Integer studentId) {
        // 查询学生是否存在
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
        
        // 查询关联的家长
        List<Association> associations = associationRepository.findByStudent(student);
        
        // 转换为ParentResponse列表
        return associations.stream()
                .map(association -> ParentResponse.fromEntity(association.getParent()))
                .collect(Collectors.toList());
    }
}