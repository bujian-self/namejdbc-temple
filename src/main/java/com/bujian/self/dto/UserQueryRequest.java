package com.bujian.self.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQueryRequest {
    private String name;
    private Integer minAge;
    private Integer maxAge;
}
