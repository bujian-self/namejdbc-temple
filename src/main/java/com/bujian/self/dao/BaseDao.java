package com.bujian.self.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.bujian.self.config.TableInfoHelp;
import com.bujian.self.dto.BaseRowMapper;
import com.bujian.self.dto.TableInfo;

import java.util.List;

/**
 * 通用 DAO 基类，封装 NamedParameterJdbcTemplate 与基于反射的通用行映射器，
 * 并通过 {@link TableInfoHelp} 反射解析实体得到表名与主键，提供基础单表查询能力。
 * 子类仅需在构造器传入实体类型即可复用。
 *
 * @param <T> 实体类型
 */
public abstract class BaseDao<T> {

    protected final NamedParameterJdbcTemplate jdbcTemplate;

    protected final BaseRowMapper<T> rowMapper;

    protected final TableInfo tableInfo;

    protected BaseDao(NamedParameterJdbcTemplate jdbcTemplate, Class<T> entityClass) {
        this.jdbcTemplate = jdbcTemplate;
        TableInfo info = TableInfoHelp.parse(entityClass);
        this.tableInfo = info;
        this.rowMapper = new BaseRowMapper<>(info);
    }

    /**
     * 根据主键查询单条记录，不存在时返回 null
     */
    public T selectById(Object id) {
        String sql = "SELECT * FROM " + tableInfo.tableName() + " WHERE " + tableInfo.idColumn() + " = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        List<T> list = jdbcTemplate.query(sql, params, rowMapper);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 根据 SQL 与参数查询唯一记录：0 条返回 null，超过 1 条抛出 IllegalStateException
     */
    public T selectOne(String sql, MapSqlParameterSource params) {
        List<T> list = selectList(sql, params);
        if (list.size() > 1) {
            throw new IllegalStateException("期望返回唯一记录，但实际返回 " + list.size() + " 条");
        }
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 根据 SQL 与参数查询记录列表。
     * 当 params 为 null 时表示无条件，执行全表查询（忽略 sql 参数）。
     */
    public List<T> selectList(String sql, MapSqlParameterSource params) {
        if (params == null) {
            params = new MapSqlParameterSource();
        }
        return jdbcTemplate.query(sql, params, rowMapper);
    }
}
