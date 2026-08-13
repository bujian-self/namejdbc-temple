package com.bujian.self.dto;

/**
 * 用户实体
 */
public record User(
    Long id,
    String name,
    Integer age,
    String email
) {
}
