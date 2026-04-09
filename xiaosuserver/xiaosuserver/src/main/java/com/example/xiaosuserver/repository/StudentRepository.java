package com.example.xiaosuserver.repository;

import com.example.xiaosuserver.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 学生数据访问接口
 * 继承JpaRepository，提供基本的CRUD操作
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {
    
    /**
     * 根据用户名查找学生
     * @param username 用户名
     * @return 学生对象，如果不存在则返回null
     */
    Student findByUsername(String username);
    
    /**
     * 根据用户名和密码查找学生
     * @param username 用户名
     * @param password 密码
     * @return 学生对象，如果不存在则返回null
     */
    Student findByUsernameAndPassword(String username, String password);
    
    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByUsername(String username);
}