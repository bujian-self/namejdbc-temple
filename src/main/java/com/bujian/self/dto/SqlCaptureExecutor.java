package com.bujian.self.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 封装 SQL 和执行结果的数据结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlCaptureExecutor<T> {
    /**
     * 捕获的 SQL 语句
     */
    private String sql;
    
    /**
     * 是否执行 SQL
     */
    private boolean shouldExecute;
    
    /**
     * 执行结果（如果执行了）
     */
    private T result;
    
    /**
     * 拒绝原因（如果没有执行）
     */
    private String rejectReason;
    
    /**
     * 创建已执行的实例
     */
    public static <T> SqlCaptureExecutor<T> executed(String sql, T result) {
        return new SqlCaptureExecutor<>(sql, true, result, null);
    }
    
    /**
     * 创建被拒绝的实例
     */
    public static <T> SqlCaptureExecutor<T> rejected(String sql, String reason) {
        return new SqlCaptureExecutor<>(sql, false, null, reason);
    }
}
