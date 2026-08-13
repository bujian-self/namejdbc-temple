package com.bujian.self.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import com.bujian.self.dao.UserDao;
import com.bujian.self.config.SqlCaptureExecutor;
import com.bujian.self.dto.User;
import com.bujian.self.dto.UserQueryRequest;

import java.util.List;

@Service
public class UserService {

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * 安全查询用户：先审查参数，再决定是否执行 SQL
     * 
     * @param request 查询请求
     * @return 查询响应
     */
    public SqlCaptureExecutor<List<User>> safeSearch(UserQueryRequest request) {
        // 构建参数
        MapSqlParameterSource params = new MapSqlParameterSource();
        
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            params.addValue("name", "%" + request.getName() + "%");
        }
        if (request.getMinAge() != null) {
            params.addValue("minAge", request.getMinAge());
        }
        if (request.getMaxAge() != null) {
            params.addValue("maxAge", request.getMaxAge());
        }
        
        // 执行查询，传入 SQL 审查函数（在 MapSqlParameterSource 层面判断）
        SqlCaptureExecutor<List<User>> executor = userDao.findUsersByCondition(
                params,
                this::reviewSqlParams
        );
        
        // 直接返回 SqlCaptureExecutor，包含 success/message/data/sql 字段
        return executor;
    }

    /**
     * SQL 审查函数：基于 MapSqlParameterSource 进行参数判断
     * 
     * @param params 参数源
     * @return 如果审查不通过返回拒绝原因，通过返回 null
     */
    private String reviewSqlParams(MapSqlParameterSource params) {
        // 检查是否有有效的查询条件
        boolean hasValidCondition = false;
        StringBuilder reasons = new StringBuilder();
        
        // 检查 name 参数
        Object nameValue = params.getValue("name");
        if (nameValue != null && nameValue.toString().length() > 2) {
            hasValidCondition = true;
        } else if (nameValue != null) {
            reasons.append("姓名查询条件至少需要 2 个字符; ");
        }
        
        // 检查年龄参数
        Object minAge = params.getValue("minAge");
        Object maxAge = params.getValue("maxAge");
        
        if (minAge != null || maxAge != null) {
            hasValidCondition = true;
            
            // 验证年龄范围合理性
            if (minAge != null && maxAge != null) {
                int min = (int) minAge;
                int max = (int) maxAge;
                if (min > max) {
                    return "最小年龄不能大于最大年龄";
                }
            }
        }
        
        // 检查是否缺少有效查询条件
        if (!hasValidCondition) {
            return "查询条件不足：请提供有效的姓名（至少 2 个字符）或年龄范围";
        }
        
        // 审查通过
        return null;
    }
}
