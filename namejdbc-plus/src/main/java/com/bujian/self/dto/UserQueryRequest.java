package com.bujian.self.dto;

/**
 * 查询请求参数
 */
public record UserQueryRequest(
    String name,
    Integer minAge,
    Integer maxAge
) {
}
