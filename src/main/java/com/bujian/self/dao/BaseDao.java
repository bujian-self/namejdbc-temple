package com.bujian.self.dao;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.bujian.self.config.TableInfoHelp;
import com.bujian.self.dto.BaseRowMapper;
import com.bujian.self.dto.QueryParam;
import com.bujian.self.dto.TableInfo;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * 通用 DAO 基类，封装 NamedParameterJdbcTemplate 与基于反射的通用行映射器，
 * 并通过 {@link TableInfoHelp} 反射解析实体得到表名与主键，提供基础单表 CRUD 能力。
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
     * 反射读取实体字段值（兼容继承字段）
     */
    private Object readField(T entity, String fieldName) {
        for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(entity);
            } catch (NoSuchFieldException ignored) {
                // 当前类无该字段，继续向父类查找
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("读取字段 " + fieldName + " 失败", e);
            }
        }
        throw new IllegalStateException("未找到字段: " + fieldName);
    }

    // ===================== 查询 =====================

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

    // ===================== 新增 =====================

    /**
     * 插入实体。自增主键字段值为 null 时自动跳过，交由数据库生成。
     *
     * @return 受影响行数
     */
    public int insert(T entity) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        MapSqlParameterSource params = new MapSqlParameterSource();
        boolean first = true;
        for (int i = 0; i < tableInfo.fieldNames().size(); i++) {
            String field = tableInfo.fieldNames().get(i);
            String col = tableInfo.columnNames().get(i);
            Object val = readField(entity, field);
            if (col.equals(tableInfo.idColumn()) && val == null) {
                continue;
            }
            if (!first) {
                columns.append(", ");
                values.append(", ");
            }
            columns.append(col);
            values.append(":").append(col);
            params.addValue(col, val);
            first = false;
        }
        String sql = "INSERT INTO " + tableInfo.tableName() + " (" + columns + ") VALUES (" + values + ")";
        return jdbcTemplate.update(sql, params);
    }

    /**
     * 根据主键更新实体非空字段。
     *
     * @return 受影响行数
     */
    public int updateById(T entity) {
        Object idVal = readField(entity, tableInfo.idField());
        if (idVal == null) {
            throw new IllegalArgumentException("更新实体缺少主键值");
        }
        StringBuilder set = new StringBuilder();
        MapSqlParameterSource params = new MapSqlParameterSource();
        boolean first = true;
        for (int i = 0; i < tableInfo.fieldNames().size(); i++) {
            String field = tableInfo.fieldNames().get(i);
            String col = tableInfo.columnNames().get(i);
            if (col.equals(tableInfo.idColumn())) {
                continue;
            }
            Object val = readField(entity, field);
            if (val == null) {
                continue;
            }
            if (!first) {
                set.append(", ");
            }
            set.append(col).append(" = :").append(col);
            params.addValue(col, val);
            first = false;
        }
        params.addValue(tableInfo.idColumn(), idVal);
        String sql = "UPDATE " + tableInfo.tableName() + " SET " + set
                + " WHERE " + tableInfo.idColumn() + " = :" + tableInfo.idColumn();
        return jdbcTemplate.update(sql, params);
    }

    /**
     * 根据主键删除。
     *
     * @return 受影响行数
     */
    public int deleteById(Object id) {
        String sql = "DELETE FROM " + tableInfo.tableName() + " WHERE " + tableInfo.idColumn() + " = :id";
        return jdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
    }

    /**
     * 根据 {@link QueryParam} 条件删除。
     *
     * @return 受影响行数
     */
    public int delete(QueryParam<T> qp) {
        String sql = "DELETE FROM " + tableInfo.tableName() + " " + qp.toWhereSql();
        return jdbcTemplate.update(sql, qp.getParams());
    }
}
