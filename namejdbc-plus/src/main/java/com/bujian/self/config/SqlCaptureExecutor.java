package com.bujian.self.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 通用 SQL 执行器：封装最终 SQL、命名参数与执行逻辑，调用方通过 {@link #execute()} 触发执行。
 * 执行前可先查看 {@link #getSql()}（参数已替换的展示 SQL）或 {@link #getRawSql()}（含 :param 占位符）。
 * 仿 MyBatis 打印 SQL 与参数日志。
 *
 * @param <R> 执行结果类型
 */
public class SqlCaptureExecutor<R> {

    private static final Logger log = LoggerFactory.getLogger(SqlCaptureExecutor.class);

    private final String sql;
    private final MapSqlParameterSource params;
    private final Supplier<R> executor;

    public SqlCaptureExecutor(String sql, MapSqlParameterSource params, Supplier<R> executor) {
        this.sql = sql;
        this.params = params;
        this.executor = executor;
    }

    public SqlCaptureExecutor(String sql, MapSqlParameterSource params, Runnable runnable) {
        this(sql, params, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 真正执行 SQL 并返回结果（执行后打印仿 MyBatis 风格日志）
     */
    public R execute() {
        R result = null;
        try {
            result = executor.get();
        } finally {
            printLog(result);
        }
        return result;
    }

    public String getRawSql() {
        return sql;
    }

    public MapSqlParameterSource getParams() {
        return params;
    }

    /**
     * 获取参数替换后的展示 SQL（仅用于日志/调试）
     */
    public String getSql() {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = Pattern.compile(":(\\w+)").matcher(sql);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            String replacement = formatValue(params.getValues().get(paramName));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private void printLog(Object res) {
        try {
            if (!log.isDebugEnabled()) {
                return;
            }
            log.debug("==>  Preparing: {} ", sql);
            log.debug("==> Parameters: {} ",
                    params.getValues().entrySet().stream()
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
