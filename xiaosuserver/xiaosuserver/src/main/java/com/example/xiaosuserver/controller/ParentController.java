package com.example.xiaosuserver.controller;

import com.example.xiaosuserver.model.ApiResponse;
import com.example.xiaosuserver.service.ParentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 家长控制器
 * 处理与家长相关的HTTP请求
 */
@RestController
@RequestMapping("/api/parents")
@CrossOrigin(origins = "*") // 允许所有来源的跨域请求
public class ParentController {

    private final ParentService parentService;

    @Autowired
    public ParentController(ParentService parentService) {
        this.parentService = parentService;
    }

    /**
     * 验证家长code
     * 
     * @param code 家长code
     * @return 验证结果
     */
    @PostMapping("/validate-code")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateParentCode(@RequestParam String code) {
        boolean isValid = parentService.validateParentCode(code);
        
        Map<String, Object> result = new HashMap<>();
        result.put("valid", isValid);
        
        if (isValid) {
            return ResponseEntity.ok(ApiResponse.success("验证成功", result));
        } else {
            return ResponseEntity.ok(ApiResponse.error("验证失败，无效的家长码", result));
        }
    }
}