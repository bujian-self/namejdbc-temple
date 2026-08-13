package com.bujian.self.config;

/**
 * 封装 SQL 和执行结果的数据结构
 */
public record SqlCaptureExecutor<T>(
    String sql,
    boolean shouldExecute,
    T result,
    String rejectReason
) {
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
