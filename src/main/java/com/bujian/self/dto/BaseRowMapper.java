package com.bujian.self.dto;

import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用行映射器：通过反射将 ResultSet 映射为实体
 * <ul>
 *   <li>实体为 record 时，按 RecordComponent 的顺序与类型调用其规范构造器创建实例</li>
 *   <li>实体为普通类时，使用无参构造创建实例后，再通过字段反射赋值</li>
 * </ul>
 * 列名与字段名按“驼峰字段名 <-> 下划线列名”规则互转匹配（例如 userName 对应 user_name）。
 *
 * @param <T> 目标实体类型
 */
public class BaseRowMapper<T> implements RowMapper<T> {

    private final Class<T> clazz;

    private final boolean record;

    public BaseRowMapper(Class<T> clazz) {
        this.clazz = clazz;
        this.record = clazz.isRecord();
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        Map<String, Integer> columnIndex = new HashMap<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columnIndex.put(meta.getColumnLabel(i).toLowerCase(), i);
        }
        return record ? mapRecord(rs, columnIndex) : mapPojo(rs, columnIndex);
    }

    private T mapRecord(ResultSet rs, Map<String, Integer> columnIndex) throws SQLException {
        RecordComponent[] components = clazz.getRecordComponents();
        Class<?>[] paramTypes = new Class<?>[components.length];
        Object[] args = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            RecordComponent rc = components[i];
            paramTypes[i] = rc.getType();
            args[i] = readValue(rs, columnIndex, rc.getName(), rc.getType());
        }
        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor(paramTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (Exception e) {
            throw new SQLException("通过反射构造 record " + clazz.getName() + " 失败", e);
        }
    }

    private T mapPojo(ResultSet rs, Map<String, Integer> columnIndex) throws SQLException {
        T instance;
        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            instance = ctor.newInstance();
        } catch (Exception e) {
            throw new SQLException("无法通过无参构造创建 " + clazz.getName() + " 实例", e);
        }
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = readValue(rs, columnIndex, field.getName(), field.getType());
                try {
                    field.set(instance, value);
                } catch (IllegalAccessException e) {
                    throw new SQLException("通过反射设置字段 " + field.getName() + " 失败", e);
                }
            }
        }
        return instance;
    }

    private Object readValue(ResultSet rs, Map<String, Integer> columnIndex, String fieldName, Class<?> type)
            throws SQLException {
        Integer idx = columnIndex.get(toColumnName(fieldName).toLowerCase());
        if (idx == null) {
            return null;
        }
        if (type == int.class || type == Integer.class) {
            int v = rs.getInt(idx);
            return rs.wasNull() ? null : v;
        }
        if (type == long.class || type == Long.class) {
            long v = rs.getLong(idx);
            return rs.wasNull() ? null : v;
        }
        if (type == double.class || type == Double.class) {
            double v = rs.getDouble(idx);
            return rs.wasNull() ? null : v;
        }
        if (type == boolean.class || type == Boolean.class) {
            boolean v = rs.getBoolean(idx);
            return rs.wasNull() ? null : v;
        }
        if (type == java.math.BigDecimal.class) {
            return rs.getBigDecimal(idx);
        }
        if (type == LocalDate.class) {
            java.sql.Date d = rs.getDate(idx);
            return d == null ? null : d.toLocalDate();
        }
        if (type == LocalDateTime.class) {
            java.sql.Timestamp t = rs.getTimestamp(idx);
            return t == null ? null : t.toLocalDateTime();
        }
        if (type == String.class) {
            return rs.getString(idx);
        }
        Object obj = rs.getObject(idx);
        return obj != null && type.isInstance(obj) ? obj : obj;
    }

    /**
     * 字段名（驼峰）转列名（下划线小写），用于按列查找 ResultSet。
     */
    private String toColumnName(String fieldName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
