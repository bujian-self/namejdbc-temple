package com.bujian.self.dto;

import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 用户实体
 */
@Table(name = "users")
public record User(
    @Id
    Long id,
    String name,
    Integer age,
    String email
) {
}
