package com.bujian.self.dao;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.bujian.self.config.SqlCaptureExecutor;
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
 * 所有执行方法均返回 {@link SqlCaptureExecutor}，由调用方通过 {@code execute()} 触发实际执行，
 * 执行前可查看生成的 SQL 与参数。
 * {@code namedParameterJdbcTemplate} 由 Spring 通过 {@link Resource} 注入，并提供 {@link #getNamedParameterJdbcTemplate()}、{@link #getTableInfo()}。
 * 子类仅需在声明泛型实体类型即可复用，构造方式有两种：
 * <ul>
 *   <li>BaseDao(Class)            —— 显式传入实体类</li>
 *   <li>BaseDao()                  —— 无参，实体类从泛型父类解析</li>
 * </ul>
 *
 * @param <T> 实体类型
 */
public abstract class BaseDao<T> {

    @Resource
    @Lazy
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    protected final TableInfo tableInfo;

    protected final BaseRowMapper<T> rowMapper;

    /**
     * 获取当前 DAO 所使用的 {@code namedParameterJdbcTemplate}。
     */
    public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
        return namedParameterJdbcTemplate;
    }

    /**
     * 获取当前 DAO 解析出的实体表元信息。
     */
    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public BaseDao(Class<T> entityClass) {
        this.tableInfo = TableInfoHelp.parse(entityClass);
        this.rowMapper = new BaseRowMapper<>(tableInfo);
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
     * 根据主键查询单条记录，不存在时返回 null。复用 {@link #selectOne(QueryParam)}。
     */
    public SqlCaptureExecutor<T> selectById(Object id) {
        QueryParam<T> qp = new QueryParam<>(tableInfo);
        qp.eq(tableInfo.idColumn(), id);
        return selectOne(qp);
    }

    /**
     * 根据 {@link QueryParam} 构造的条件查询记录列表（自动生成 SELECT * FROM 表 WHERE ... ORDER BY ...）
     */
    public SqlCaptureExecutor<List<T>> selectList(QueryParam<T> qp) {
        String sql = "SELECT * FROM " + tableInfo.tableName() + " " + qp.toSql();
        MapSqlParameterSource params = qp.getParams();
        return new SqlCaptureExecutor<>(sql, params, () -> namedParameterJdbcTemplate.query(sql, params, rowMapper));
    }

    /**
     * 根据 {@link QueryParam} 查询唯一记录：0 条返回 null，超过 1 条抛异常
     */
    public SqlCaptureExecutor<T> selectOne(QueryParam<T> qp) {
        String sql = "SELECT * FROM " + tableInfo.tableName() + " " + qp.toSql();
        MapSqlParameterSource params = qp.getParams();
        return new SqlCaptureExecutor<>(sql, params, () -> {
            List<T> list = namedParameterJdbcTemplate.query(sql, params, rowMapper);
            if (list.size() > 1) {
                throw new IllegalStateException("期望返回唯一记录，但实际返回 " + list.size() + " 条");
            }
            return list.isEmpty() ? null : list.get(0);
        });
    }

    /**
     * 根据 {@link QueryParam} 查询满足条件的总记录数
     */
    public SqlCaptureExecutor<Long> selectCount(QueryParam<T> qp) {
        String sql = "SELECT COUNT(*) FROM " + tableInfo.tableName() + " " + qp.toWhereSql();
        MapSqlParameterSource params = qp.getParams();
        return new SqlCaptureExecutor<>(sql, params, () -> {
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count == null ? 0L : count;
        });
    }

    // ===================== 新增 =====================

    /**
     * 插入实体。自增主键字段值为 null 时自动跳过，交由数据库生成。
     *
     * @return 封装受影响行数的执行器
     */
    public SqlCaptureExecutor<Integer> insert(T entity) {
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
        return new SqlCaptureExecutor<>(sql, params, () -> namedParameterJdbcTemplate.update(sql, params));
    }

    /**
     * 根据主键更新实体非空字段。
     *
     * @return 封装受影响行数的执行器
     */
    public SqlCaptureExecutor<Integer> updateById(T entity) {
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
        return new SqlCaptureExecutor<>(sql, params, () -> namedParameterJdbcTemplate.update(sql, params));
    }

    /**
     * 根据主键删除。复用 {@link #delete(QueryParam)}。
     *
     * @return 封装受影响行数的执行器
     */
    public SqlCaptureExecutor<Integer> deleteById(Object id) {
        QueryParam<T> qp = new QueryParam<>(tableInfo);
        qp.eq(tableInfo.idColumn(), id);
        return delete(qp);
    }

    /**
     * 根据 {@link QueryParam} 条件删除。
     *
     * @return 封装受影响行数的执行器
     */
    public SqlCaptureExecutor<Integer> delete(QueryParam<T> qp) {
        String sql = "DELETE FROM " + tableInfo.tableName() + " " + qp.toWhereSql();
        MapSqlParameterSource params = qp.getParams();
        return new SqlCaptureExecutor<>(sql, params, () -> namedParameterJdbcTemplate.update(sql, params));
    }
}