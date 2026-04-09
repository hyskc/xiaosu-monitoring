package com.example.xiaosuparentserver.controller;

import com.example.xiaosuparentserver.dto.ApiResponse;
import com.example.xiaosuparentserver.dto.LoginRequest;
import com.example.xiaosuparentserver.dto.LoginResponse;
import com.example.xiaosuparentserver.service.ParentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parents")
public class ParentController {

    @Autowired
    private ParentService parentService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = parentService.login(loginRequest);
        
        if (loginResponse.isSuccess()) {
            return ApiResponse.success(loginResponse.getMessage(), loginResponse);
        } else {
            return ApiResponse.fail(loginResponse.getMessage());
        }
    }
}