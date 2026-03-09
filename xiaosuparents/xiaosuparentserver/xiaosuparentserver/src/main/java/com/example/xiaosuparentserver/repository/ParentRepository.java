package com.example.xiaosuparentserver.repository;

import com.example.xiaosuparentserver.entity.Parent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ParentRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Parent> parentRowMapper = new RowMapper<Parent>() {
        @Override
        public Parent mapRow(ResultSet rs, int rowNum) throws SQLException {
            Parent parent = new Parent();
            parent.setId(rs.getInt("id"));
            parent.setUsername(rs.getString("username"));
            parent.setPassword(rs.getString("password"));
            parent.setCode(rs.getString("code"));
            return parent;
        }
    };

    public Parent findByUsername(String username) {
        String sql = "SELECT * FROM parents WHERE username = ?";
        List<Parent> parents = jdbcTemplate.query(sql, parentRowMapper, username);
        return parents.isEmpty() ? null : parents.get(0);
    }

    public Parent findByUsernameAndPassword(String username, String password) {
        String sql = "SELECT * FROM parents WHERE username = ? AND password = ?";
        List<Parent> parents = jdbcTemplate.query(sql, parentRowMapper, username, password);
        return parents.isEmpty() ? null : parents.get(0);
    }
    
    public Parent findById(Integer id) {
        String sql = "SELECT * FROM parents WHERE id = ?";
        List<Parent> parents = jdbcTemplate.query(sql, parentRowMapper, id);
        return parents.isEmpty() ? null : parents.get(0);
    }
}