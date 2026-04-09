package com.example.xiaosuparentserver.repository;

import com.example.xiaosuparentserver.entity.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class StudentRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Student> studentRowMapper = new RowMapper<Student>() {
        @Override
        public Student mapRow(ResultSet rs, int rowNum) throws SQLException {
            Student student = new Student();
            student.setId(rs.getInt("id"));
            student.setUsername(rs.getString("username"));
            student.setPassword(rs.getString("password"));
            student.setActive(rs.getBoolean("active"));
            return student;
        }
    };

    public Student findByUsername(String username) {
        String sql = "SELECT * FROM students WHERE username = ?";
        List<Student> students = jdbcTemplate.query(sql, studentRowMapper, username);
        return students.isEmpty() ? null : students.get(0);
    }

    public Student findByUsernameAndPassword(String username, String password) {
        String sql = "SELECT * FROM students WHERE username = ? AND password = ?";
        List<Student> students = jdbcTemplate.query(sql, studentRowMapper, username, password);
        return students.isEmpty() ? null : students.get(0);
    }

    public Student findById(Integer id) {
        String sql = "SELECT * FROM students WHERE id = ?";
        List<Student> students = jdbcTemplate.query(sql, studentRowMapper, id);
        return students.isEmpty() ? null : students.get(0);
    }
}