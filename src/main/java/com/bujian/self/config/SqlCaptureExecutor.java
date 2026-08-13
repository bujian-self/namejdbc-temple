package com.bujian.self.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询响应结果 - 兼容 graceful-response 格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlCaptureExecutor<T> {
    /**
     * 是否执行 SQL（对应 graceful-response 的 code: 0=成功，非 0=失败）
     */
    private boolean shouldExecute;
    
    /**
     * 消息（对应 graceful-response 的 msg）
     */
    private String message;
    
    /**
     * 执行结果/数据（对应 graceful-response 的 data）
     */
    private T data;
    
    /**
     * 捕获的 SQL 语句（额外字段）
     */
    private String sql;
    
    /**
     * 拒绝原因（如果没有执行）
     */
    private String rejectReason;
    
    /**
     * 创建已执行的实例
     */
    public static <T> SqlCaptureExecutor<T> executed(String sql, T result) {
        return new SqlCaptureExecutor<>(true, "查询成功", result, sql, null);
    }
    
    /**
     * 创建被拒绝的实例
     */
    public static <T> SqlCaptureExecutor<T> rejected(String sql, String reason) {
        return new SqlCaptureExecutor<>(false, reason, null, sql, reason);
    }
    
    // 为了兼容原有代码的 getter 方法
    public boolean isShouldExecute() {
        return shouldExecute;
    }
    
    public T getResult() {
        return data;
    }
    
    public String getSql() {
        return sql;
    }
    
    public String getRejectReason() {
        return rejectReason;
    }
}
