package com.bujian.self.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bujian.self.config.SqlCaptureExecutor;
import com.bujian.self.dto.User;

import java.util.List;

@Repository
public class UserDao extends BaseDao<User> {

    public UserDao(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, User.class);
    }

    /**
     * 根据条件查询用户（参数校验由 service 层完成）
     *
     * @param params 查询参数
     * @return 封装了 SQL 和执行所需上下文的对象，由调用方决定何时执行
     */
    public SqlCaptureExecutor<List<User>> findUsersByCondition(MapSqlParameterSource params) {

        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT id, name, age, email FROM users WHERE 1=1");

        if (params.getValues().get("name") != null) {
            sqlBuilder.append(" AND name LIKE :name");
        }
        if (params.getValues().get("minAge") != null) {
            sqlBuilder.append(" AND age >= :minAge");
        }
        if (params.getValues().get("maxAge") != null) {
            sqlBuilder.append(" AND age <= :maxAge");
        }

        // 返回可执行对象，由调用方决定何时执行（sqlbuild 构造：sqlBuilder、params、queryFunction）
        String sql = sqlBuilder.toString();
        return new SqlCaptureExecutor<>(
                sql,
                params,
                () -> jdbcTemplate.query(sql, params, rowMapper)
        );
    }
}
