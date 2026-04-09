package com.example.xiaosuserver.service.impl;

import com.example.xiaosuserver.entity.Parent;
import com.example.xiaosuserver.repository.ParentRepository;
import com.example.xiaosuserver.service.ParentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 家长服务实现类
 */
@Service
public class ParentServiceImpl implements ParentService {

    private final ParentRepository parentRepository;

    @Autowired
    public ParentServiceImpl(ParentRepository parentRepository) {
        this.parentRepository = parentRepository;
    }

    /**
     * 根据家长code验证家长身份
     *
     * @param code 家长code
     * @return 验证结果，true表示验证通过，false表示验证失败
     */
    @Override
    public boolean validateParentCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        // 查询数据库中是否存在该code
        Parent parent = parentRepository.findByCode(code);
        return parent != null;
    }

    /**
     * 根据code查找家长
     *
     * @param code 家长code
     * @return 家长实体，如果不存在则返回null
     */
    @Override
    public Parent findByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return parentRepository.findByCode(code);
    }
}