package com.bujian.self.config;

import com.bujian.self.dto.TableInfo;

import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 实体元信息解析工具：通过反射从实体类（record 或普通 POJO）解析出表名、字段/列映射与主键，
 * 生成 {@link TableInfo} 供通用 DAO 使用。
 * 优先读取 {@link Table} / {@link Id} 注解，未标注时回退到类名推导与 id 字段约定。
 */
public class TableInfoHelp {

    /**
     * 解析实体类，返回表元信息
     */
    public static TableInfo parse(Class<?> clazz) {
        Map<String, String> fieldToColumn = new LinkedHashMap<>();
        Map<String, Class<?>> fieldTypes = new LinkedHashMap<>();
        List<String> fieldNames = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();
        String idField = null;
        String idColumn = null;

        List<String> names = new ArrayList<>();
        Map<String, Class<?>> types = new LinkedHashMap<>();
        if (clazz.isRecord()) {
            for (RecordComponent rc : clazz.getRecordComponents()) {
                names.add(rc.getName());
                types.put(rc.getName(), rc.getType());
            }
        } else {
            for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    names.add(f.getName());
                    types.put(f.getName(), f.getType());
                }
            }
        }

        for (String fn : names) {
            String cn = toColumnName(fn);
            fieldNames.add(fn);
            columnNames.add(cn);
            fieldToColumn.put(fn, cn);
            fieldTypes.put(fn, types.get(fn));
            try {
                if (clazz.getDeclaredField(fn).isAnnotationPresent(Id.class)) {
                    idField = fn;
                    idColumn = cn;
                }
            } catch (NoSuchFieldException ignored) {
                // 字段不可见时忽略，沿用默认主键约定
            }
        }

        Table tableAnno = clazz.getAnnotation(Table.class);
        String tableName = (tableAnno != null && !tableAnno.name().isEmpty())
                ? tableAnno.name()
                : toTableName(clazz);

        return new TableInfo(clazz, tableName, idField, idColumn,
                fieldNames, columnNames, fieldToColumn, fieldTypes);
    }

    /**
     * 字段名（驼峰）转列名（下划线小写）
     */
    private static String toColumnName(String fieldName) {
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

    /**
     * 由类名推导表名：去除常见实体后缀后驼峰转下划线，并简单复数化（加 s）。
     * 例如 User -> user -> users。仅在实体未标注 {@link Table} 时使用。
     */
    private static String toTableName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        name = name.replaceAll("(Entity|DO|DTO|Bo|Po)$", "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString() + "s";
    }
}
