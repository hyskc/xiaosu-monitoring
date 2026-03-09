package com.example.xiaosuparentserver.service;

import com.example.xiaosuparentserver.dto.LoginRequest;
import com.example.xiaosuparentserver.dto.LoginResponse;
import com.example.xiaosuparentserver.entity.Parent;
import com.example.xiaosuparentserver.repository.ParentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ParentService {

    @Autowired
    private ParentRepository parentRepository;

    public LoginResponse login(LoginRequest loginRequest) {
        LoginResponse response = new LoginResponse();
        
        // 参数校验
        if (loginRequest.getUsername() == null || loginRequest.getUsername().isEmpty() ||
                loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
            response.setSuccess(false);
            response.setMessage("用户名和密码不能为空");
            return response;
        }
        
        // 查询用户
        Parent parent = parentRepository.findByUsernameAndPassword(
                loginRequest.getUsername(), loginRequest.getPassword());
        
        // 验证结果
        if (parent == null) {
            response.setSuccess(false);
            response.setMessage("用户名或密码错误");
            return response;
        }
        
        // 登录成功
        response.setSuccess(true);
        response.setMessage("登录成功");
        response.setUserId(parent.getId());
        response.setUsername(parent.getUsername());
        response.setCode(parent.getCode());
        
        return response;
    }
}