package com.example.xiaosuserver.repository;

import com.example.xiaosuserver.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 家长数据访问接口
 */
@Repository
public interface ParentRepository extends JpaRepository<Parent, Integer> {
    
    /**
     * 根据code查找家长
     * 
     * @param code 家长code
     * @return 家长实体，如果不存在则返回null
     */
    Parent findByCode(String code);
}