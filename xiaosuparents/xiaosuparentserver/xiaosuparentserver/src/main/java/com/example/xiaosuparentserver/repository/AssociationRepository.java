package com.example.xiaosuparentserver.repository;

import com.example.xiaosuparentserver.entity.Association;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
public class AssociationRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Association> associationRowMapper = new RowMapper<Association>() {
        @Override
        public Association mapRow(ResultSet rs, int rowNum) throws SQLException {
            Association association = new Association();
            association.setId(rs.getInt("id"));
            association.setParentId(rs.getInt("parentid"));
            association.setStudentId(rs.getInt("studentid"));
            return association;
        }
    };

    public Integer save(Association association) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO association (parentid, studentid) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, association.getParentId());
            ps.setInt(2, association.getStudentId());
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().intValue() : null;
    }

    public Association findByParentIdAndStudentId(Integer parentId, Integer studentId) {
        String sql = "SELECT * FROM association WHERE parentid = ? AND studentid = ?";
        List<Association> associations = jdbcTemplate.query(sql, associationRowMapper, parentId, studentId);
        return associations.isEmpty() ? null : associations.get(0);
    }
    
    public List<Association> findByParentId(Integer parentId) {
        String sql = "SELECT * FROM association WHERE parentid = ?";
        return jdbcTemplate.query(sql, associationRowMapper, parentId);
    }
    
    /**
     * 删除家长与学生的关联关系
     * @param parentId 家长ID
     * @param studentId 学生ID
     * @return 受影响的行数
     */
    public int deleteByParentIdAndStudentId(Integer parentId, Integer studentId) {
        String sql = "DELETE FROM association WHERE parentid = ? AND studentid = ?";
        return jdbcTemplate.update(sql, parentId, studentId);
    }
}