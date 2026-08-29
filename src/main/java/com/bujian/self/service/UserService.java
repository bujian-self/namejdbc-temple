package com.bujian.self.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import com.bujian.self.config.SqlCaptureExecutor;
import com.bujian.self.dao.UserDao;
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
    public List<User> safeSearch(UserQueryRequest request) {
        // 构建参数
        MapSqlParameterSource params = new MapSqlParameterSource();
        
        if (request.name() != null && !request.name().trim().isEmpty()) {
            params.addValue("name", "%" + request.name() + "%");
        }
        if (request.minAge() != null) {
            params.addValue("minAge", request.minAge());
        }
        if (request.maxAge() != null) {
            params.addValue("maxAge", request.maxAge());
        }
        
        // 先由 service 自行校验参数（基于 MapSqlParameterSource 判断），校验不通过时抛出异常
        String rejectReason = reviewSqlParams(params);
        if (rejectReason != null) {
            throw new IllegalArgumentException(rejectReason);
        }
        
        // 校验通过，执行查询并返回结果（由调用方决定何时执行）
        SqlCaptureExecutor<List<User>> executor = userDao.findUsersByCondition(params);
        return executor.execute();
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
        
        // 检查 name 参数（getValues().get 在参数未注册时返回 null，不会抛异常）
        Object nameValue = params.getValues().get("name");
        if (nameValue != null && nameValue.toString().length() > 2) {
            hasValidCondition = true;
        } else if (nameValue != null) {
            reasons.append("姓名查询条件至少需要 2 个字符; ");
        }
        
        // 检查年龄参数（getValues().get 在参数未注册时返回 null，不会抛异常）
        Object minAge = params.getValues().get("minAge");
        Object maxAge = params.getValues().get("maxAge");
        
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
