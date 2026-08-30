package com.bujian.self.dao;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.bujian.self.config.TableInfoHelp;
import com.bujian.self.dto.BaseRowMapper;
import com.bujian.self.dto.QueryParam;
import com.bujian.self.dto.TableInfo;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * 通用 DAO 基类，封装 NamedParameterJdbcTemplate 与基于反射的通用行映射器，
 * 并通过 {@link TableInfoHelp} 反射解析实体得到表名与主键，提供基础单表查询能力。
 * 子类仅需在声明泛型实体类型即可复用，构造方式有三种：
 * <ul>
 *   <li>BaseDao(jdbcTemplate, Class)            —— 显式传入 jdbcTemplate 与实体类（最可靠）</li>
 *   <li>BaseDao(jdbcTemplate)                   —— 实体类从泛型父类解析</li>
 *   <li>BaseDao()                              —— 无参，jdbcTemplate 由 @Resource @Lazy 字段注入</li>
 * </ul>
 *
 * @param <T> 实体类型
 */
public abstract class BaseDao<T> {

    @Resource(name = "namedParameterJdbcTemplate")
    @Lazy
    protected NamedParameterJdbcTemplate jdbcTemplate;

    protected final TableInfo tableInfo;

    protected final BaseRowMapper<T> rowMapper;

    public BaseDao(NamedParameterJdbcTemplate jdbcTemplate, Class<T> entityClass) {
        this.tableInfo = TableInfoHelp.parse(entityClass);
        this.rowMapper = new BaseRowMapper<>(tableInfo);
        this.jdbcTemplate = jdbcTemplate;
    }

    public BaseDao(NamedParameterJdbcTemplate jdbcTemplate) {
        Class<T> entityClass = resolveEntityClass();
        this.tableInfo = TableInfoHelp.parse(entityClass);
        this.rowMapper = new BaseRowMapper<>(tableInfo);
        this.jdbcTemplate = jdbcTemplate;
    }

    protected BaseDao() {
        Class<T> entityClass = resolveEntityClass();
        this.tableInfo = TableInfoHelp.parse(entityClass);
        this.rowMapper = new BaseRowMapper<>(tableInfo);
    }

    /**
     * 从泛型父类解析实际实体类型，兼容 Spring CGLIB 代理（向上遍历父类链）。
     */
    @SuppressWarnings("unchecked")
    private Class<T> resolveEntityClass() {
        Class<?> current = getClass();
        while (current != null && current != Object.class) {
            Type superClass = current.getGenericSuperclass();
            if (superClass instanceof ParameterizedType pt && pt.getRawType() == BaseDao.class) {
                Type arg = pt.getActualTypeArguments()[0];
                if (arg instanceof Class<?> clazz) {
                    return (Class<T>) clazz;
                }
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("无法从泛型父类解析实体类型: " + getClass().getName());
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
     * 当 params 为 null 时重新赋值为 new MapSqlParameterSource()，照常执行传入的 sql。
     */
    public List<T> selectList(String sql, MapSqlParameterSource params) {
        if (params == null) {
            params = new MapSqlParameterSource();
        }
        return jdbcTemplate.query(sql, params, rowMapper);
    }

    /**
     * 根据 {@link QueryParam} 构造的条件查询记录列表（自动生成 SELECT * FROM 表 WHERE ... ORDER BY ...）
     */
    public List<T> selectList(QueryParam<T> qp) {
        String sql = "SELECT * FROM " + tableInfo.tableName() + " " + qp.toSql();
        return jdbcTemplate.query(sql, qp.getParams(), rowMapper);
    }

    /**
     * 根据 {@link QueryParam} 查询唯一记录：0 条返回 null，超过 1 条抛异常
     */
    public T selectOne(QueryParam<T> qp) {
        List<T> list = selectList(qp);
        if (list.size() > 1) {
            throw new IllegalStateException("期望返回唯一记录，但实际返回 " + list.size() + " 条");
        }
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 根据 {@link QueryParam} 查询满足条件的总记录数
     */
    public long selectCount(QueryParam<T> qp) {
        String sql = "SELECT COUNT(*) FROM " + tableInfo.tableName() + " " + qp.toWhereSql();
        Long count = jdbcTemplate.queryForObject(sql, qp.getParams(), Long.class);
        return count == null ? 0L : count;
    }
}
