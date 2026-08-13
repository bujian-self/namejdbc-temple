package com.bujian.self.service;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import com.bujian.self.config.SqlCaptureExecutor;

import java.util.List;
import java.util.function.Function;

/**
 * 扩展 NamedParameterJdbcTemplate，支持捕获 SQL 并决定是否执行
 */
public class SqlCapturingNamedParameterJdbcTemplate {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SqlCapturingNamedParameterJdbcTemplate(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询方法：先捕获 SQL，然后根据业务逻辑决定是否执行
     * 
     * @param sql SQL 语句
     * @param params 参数源（用于 SQL 审查）
     * @param rowMapper 行映射器
     * @param <T> 返回类型
     * @return 封装了 SQL 和执行结果的对象
     */
    public <T> SqlCaptureExecutor<List<T>> query(String sql, 
                                                  MapSqlParameterSource params,
                                                  RowMapper<T> rowMapper,
                                                  Function<MapSqlParameterSource, String> sqlReviewFunction) {
        // 生成实际 SQL（用于展示）
        String actualSql = generateActualSql(sql, params);
        
        // 先进行 SQL 审查（基于参数判断）
        String rejectReason = sqlReviewFunction.apply(params);
        
        if (rejectReason != null) {
            // 审查不通过，拒绝执行
            return SqlCaptureExecutor.rejected(actualSql, rejectReason);
        }
        
        // 审查通过，执行查询
        List<T> result = jdbcTemplate.query(sql, params, rowMapper);
        return SqlCaptureExecutor.executed(actualSql, result);
    }

    /**
     * 生成实际 SQL（将参数替换到 SQL 中，仅用于展示）
     */
    private String generateActualSql(String sql, SqlParameterSource params) {
        // 简单实现：返回原始 SQL 和参数信息
        // 实际项目中可以使用 SQL 日志库来生成完整的 SQL
        StringBuilder sb = new StringBuilder();
        sb.append("SQL: ").append(sql).append("\n");
        sb.append("Parameters: ").append(params);
        return sb.toString();
    }
}
