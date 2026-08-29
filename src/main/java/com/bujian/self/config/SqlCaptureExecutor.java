package com.bujian.self.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * 封装 SQL 和执行所需上下文的数据结构
 *
 * @param <R> 执行函数的返回类型
 */
public record SqlCaptureExecutor<R>(
    String sql,
    MapSqlParameterSource params,
    Supplier<R> queryFunction
) {
    static final Logger log = LoggerFactory.getLogger(SqlCaptureExecutor.class);

    /**
     * 构造函数：用于传递无返回值的执行函数（如 INSERT/UPDATE/DELETE）
     */
    public SqlCaptureExecutor(String sql,
                              MapSqlParameterSource params,
                              Runnable updateFunction) {
        this(sql, params, () -> {
            updateFunction.run();
            return null;
        });
    }

    /**
     * 执行查询并返回结果（使用无返回值函数时返回 null）
     */
    public R execute() {
        R result = null;
        try {
            result = queryFunction.get();
            return result;
        } finally {
            printLog(result);
        }
    }

    /**
     * SQL 执行日志，仿 mybatis 风格
     */
    private void printLog(Object res) {
        try {
            if (!log.isDebugEnabled()) {
                return;
            }
            log.debug("==>  Preparing: {} ", sql);
            log.debug("==> Parameters: {} ", params.getValues().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue()
                            + "(" + (e.getValue() == null ? "null" : e.getValue().getClass().getSimpleName()) + ")")
                    .collect(Collectors.joining(", ")));
            Integer total = null;
            if (res instanceof Integer) {
                total = (Integer) res;
            } else if (res instanceof List<?> list) {
                total = list.size();
                if (log.isTraceEnabled()) {
                    for (Object row : list) {
                        log.trace("<==        Row: {} ", row);
                    }
                }
            }
            log.debug("<==      Total: {} ", total != null ? total : "");
        } catch (Exception e) {
            log.debug("sql print log error", e);
        }
    }

    /**
     * 获取生成的最终 SQL（将参数替换进 SQL，仅用于展示）
     */
    public String getSql() {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = java.util.regex.Pattern.compile(":(\\w+)").matcher(sql);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            String replacement = formatValue(params.getValues().get(paramName));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 将参数值格式化为 SQL 字面量
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }
}
