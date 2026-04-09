package com.example.xiaosuserver.service;

import com.example.xiaosuserver.entity.Parent;

/**
 * 家长服务接口
 */
public interface ParentService {
    
    /**
     * 根据家长code验证家长身份
     *
     * @param code 家长code
     * @return 验证结果，true表示验证通过，false表示验证失败
     */
    boolean validateParentCode(String code);
    
    /**
     * 根据code查找家长
     *
     * @param code 家长code
     * @return 家长实体，如果不存在则返回null
     */
    Parent findByCode(String code);
}