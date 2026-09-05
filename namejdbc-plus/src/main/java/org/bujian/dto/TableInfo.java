package org.bujian.dto;

import org.bujian.config.TableInfoHelp;

import java.util.List;
import java.util.Map;

/**
 * 实体对应的表元信息，由 {@link TableInfoHelp} 通过反射解析得到。
 */
public record TableInfo(
        Class<?> entityClass,
        String tableName,
        String idField,
        String idColumn,
        List<String> fieldNames,
        List<String> columnNames,
        Map<String, String> fieldToColumn,
        Map<String, Class<?>> fieldTypes
) {
}
