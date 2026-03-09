package com.example.xiaosuserver.controller;

import com.example.xiaosuserver.entity.Student;
import com.example.xiaosuserver.model.ApiResponse;
import com.example.xiaosuserver.model.ParentResponse;
import com.example.xiaosuserver.service.AssociationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关联账号控制器
 * 处理学生与家长关联关系的HTTP请求
 */
@RestController
@RequestMapping("/api/association")
@CrossOrigin(origins = "*") // 允许所有来源的跨域请求
public class AssociationController {

    private final AssociationService associationService;

    @Autowired
    public AssociationController(AssociationService associationService) {
        this.associationService = associationService;
    }

    /**
     * 获取学生关联的家长账号
     * @param studentId 学生ID
     * @return 关联的家长账号列表
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<ParentResponse>>> getAssociatedParents(@PathVariable Integer studentId) {
        List<ParentResponse> parents = associationService.getAssociatedParents(studentId);
        
        if (parents.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("该学生暂无关联家长账号", parents));
        }
        
        return ResponseEntity.ok(ApiResponse.success("获取关联家长账号成功", parents));
    }
}