package com.bujian.self.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询响应结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String sql;
    
    public static <T> QueryResponse<T> success(T data, String sql) {
        return new QueryResponse<>(true, "查询成功", data, sql);
    }
    
    public static <T> QueryResponse<T> rejected(String message, String sql) {
        return new QueryResponse<>(false, message, null, sql);
    }
}
