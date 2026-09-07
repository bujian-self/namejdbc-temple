package org.bujian.config;

import org.bujian.dto.QueryParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
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
    private static final Pattern PARAM_PATTERN = Pattern.compile(":([a-zA-Z_]\\w*)");

    private final String sql;
    private final MapSqlParameterSource params;
    private final Supplier<R> executor;
    private final long startTime;
    private final boolean logElapsed;

    public SqlCaptureExecutor(String sql, MapSqlParameterSource params, Supplier<R> executor) {
        this(sql, params, executor, true);
    }

    public SqlCaptureExecutor(String sql, MapSqlParameterSource params, Supplier<R> executor, boolean logElapsed) {
        this.sql = sql;
        this.params = params;
        this.executor = executor;
        this.startTime = System.currentTimeMillis();
        this.logElapsed = logElapsed;
    }

    public SqlCaptureExecutor(QueryParam<?> qp, Supplier<R> executor) {
        this(qp.toSql(), qp.getParams(), executor);
    }

    public SqlCaptureExecutor(QueryParam<?> qp, Runnable runnable) {
        this(qp.toSql(), qp.getParams(), () -> {
            runnable.run();
            return null;
        });
    }

    public SqlCaptureExecutor(String sql, MapSqlParameterSource params, Runnable runnable) {
        this(sql, params, () -> {
            runnable.run();
            return null;
        });
    }

    public R execute() {
        R result = null;
        try {
            result = executor.get();
            return result;
        } catch (Exception e) {
            log.error("SQL 执行异常: {} | 参数: {}", sql, formatParams(), e);
            throw e;
        } finally {
            printLog(result);
        }
    }

    public String getRawSql() {
        return sql;
    }

    public MapSqlParameterSource getParams() {
        return params;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    public String getSql() {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = PARAM_PATTERN.matcher(sql);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = params.getValues().get(paramName);
            if (value != null) {
                String replacement = formatValue(value);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private void printLog(Object res) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("==>  Preparing: {} ", sql);
        log.debug("==> Parameters: {}", formatParams());
        if (logElapsed) {
            log.debug("==>  Cost: {} ms", System.currentTimeMillis() - startTime);
        }
        Integer total = resolveTotal(res);
        if (total != null) {
            if (log.isTraceEnabled() && res instanceof List<?> list) {
                list.forEach(row -> log.trace("<==        Row: {} ", row));
            }
            log.debug("<==      Total: {}", total);
        }
    }

    private String formatParams() {
        return params.getValues().entrySet().stream()
                .map(e -> e.getKey() + "=" + formatValue(e.getValue()))
                .collect(Collectors.joining(", "));
    }

    private Integer resolveTotal(Object res) {
        if (res instanceof Integer) {
            return (Integer) res;
        }
        if (res instanceof List<?> list) {
            return list.size();
        }
        return null;
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Date) {
            return "'" + value + "'";
        }
        if (value instanceof LocalDate) {
            return "'" + value + "'";
        }
        if (value instanceof LocalTime) {
            return "'" + value + "'";
        }
        if (value instanceof LocalDateTime) {
            return "'" + value + "'";
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }

    @Override
    public String toString() {
        return "SqlCaptureExecutor{" +
                "sql='" + sql + '\'' +
                ", elapsed=" + getElapsedTime() + "ms" +
                '}';
    }
}
