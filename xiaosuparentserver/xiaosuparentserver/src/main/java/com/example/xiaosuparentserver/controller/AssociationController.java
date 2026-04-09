package com.example.xiaosuparentserver.controller;

import com.example.xiaosuparentserver.dto.ApiResponse;
import com.example.xiaosuparentserver.dto.AssociationRequest;
import com.example.xiaosuparentserver.entity.Student;
import com.example.xiaosuparentserver.service.AssociationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/association")
public class AssociationController {

    @Autowired
    private AssociationService associationService;

    /**
     * 验证学生账号密码并关联到家长
     * @param request 关联请求
     * @return 关联结果
     */
    @PostMapping("/associate")
    public ApiResponse<String> associateStudentToParent(@RequestBody AssociationRequest request) {
        return associationService.associateStudentToParent(request);
    }
    
    /**
     * 获取家长关联的学生列表
     * @param parentId 家长ID
     * @return 学生列表
     */
    @GetMapping("/students/{parentId}")
    public ApiResponse<List<Student>> getAssociatedStudents(@PathVariable Integer parentId) {
        return associationService.getAssociatedStudents(parentId);
    }
    
    /**
     * 解除家长与学生的关联
     * @param parentId 家长ID
     * @param studentId 学生ID
     * @return 解除关联结果
     */
    @DeleteMapping("/{parentId}/{studentId}")
    public ApiResponse<String> unlinkStudent(
            @PathVariable Integer parentId,
            @PathVariable Integer studentId) {
        return associationService.unlinkStudent(parentId, studentId);
    }
}