package com.example.xiaosuserver.repository;

import com.example.xiaosuserver.entity.Association;
import com.example.xiaosuserver.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 关联关系的数据访问接口
 */
@Repository
public interface AssociationRepository extends JpaRepository<Association, Integer> {
    
    /**
     * 根据学生ID查询所有关联的家长
     * 
     * @param student 学生实体
     * @return 关联的家长列表
     */
    List<Association> findByStudent(Student student);
    
    /**
     * 根据学生ID查询所有关联的家长ID
     * 
     * @param studentId 学生ID
     * @return 关联的家长列表
     */
    @Query("SELECT a FROM Association a WHERE a.student.id = :studentId")
    List<Association> findByStudentId(@Param("studentId") Integer studentId);
}