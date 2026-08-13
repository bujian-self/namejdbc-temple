package com.bujian.self.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import com.bujian.self.dto.SqlCaptureExecutor;
import com.bujian.self.dto.User;
import com.bujian.self.dto.UserRowMapper;
import com.bujian.self.service.SqlCapturingNamedParameterJdbcTemplate;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Repository
public class UserDao {

    private final SqlCapturingNamedParameterJdbcTemplate jdbcTemplate;

    public UserDao(SqlCapturingNamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 根据条件查询用户
     * 
     * @param params 查询参数
     * @param sqlReviewFunction SQL 审查函数
     * @return 封装了 SQL 和执行结果的对象
     */
    public SqlCaptureExecutor<List<User>> findUsersByCondition(
            MapSqlParameterSource params,
            Function<MapSqlParameterSource, String> sqlReviewFunction) {
        
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT id, name, age, email FROM users WHERE 1=1");
        
        if (params.getValue("name") != null) {
            sqlBuilder.append(" AND name LIKE :name");
        }
        if (params.getValue("minAge") != null) {
            sqlBuilder.append(" AND age >= :minAge");
        }
        if (params.getValue("maxAge") != null) {
            sqlBuilder.append(" AND age <= :maxAge");
        }
        
        return jdbcTemplate.query(
                sqlBuilder.toString(),
                params,
                new UserRowMapper(),
                sqlReviewFunction
        );
    }
}
