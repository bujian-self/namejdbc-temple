package com.bujian.self.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.bujian.self.dto.BaseRowMapper;

import java.util.List;

/**
 * 通用 DAO 基类，封装 NamedParameterJdbcTemplate 与基于反射的通用行映射器。
 * 子类只需通过构造器传入实体类型，即可复用 rowMapper 与通用查询能力。
 *
 * @param <T> 实体类型
 */
public abstract class BaseDao<T> {

    protected final NamedParameterJdbcTemplate jdbcTemplate;

    protected final BaseRowMapper<T> rowMapper;

    protected BaseDao(NamedParameterJdbcTemplate jdbcTemplate, Class<T> entityClass) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = new BaseRowMapper<>(entityClass);
    }

    /**
     * 通用查询：执行 SQL 并返回实体列表
     */
    protected List<T> query(String sql, MapSqlParameterSource params) {
        return jdbcTemplate.query(sql, params, rowMapper);
    }
}
