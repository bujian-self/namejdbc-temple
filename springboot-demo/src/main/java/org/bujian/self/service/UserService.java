package org.bujian.self.service;

import org.bujian.dto.QueryParam;
import org.bujian.dto.UserQueryRequest;
import org.springframework.stereotype.Service;

import org.bujian.self.dao.UserDao;
import org.bujian.self.dto.User;

import java.util.List;

@Service
public class UserService {

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * 安全查询用户：先审查参数，再基于 {@link QueryParam} 执行查询。
     */
    public List<User> safeSearch(UserQueryRequest request) {
        String rejectReason = reviewRequest(request);
        if (rejectReason != null) {
            throw new IllegalArgumentException(rejectReason);
        }
        QueryParam<User> qp = new QueryParam<>();
        if (request.name() != null && !request.name().trim().isEmpty()) {
            qp = qp.like("name", request.name());
        }
        if (request.minAge() != null) {
            qp = qp.ge("age", request.minAge());
        }
        if (request.maxAge() != null) {
            qp = qp.le("age", request.maxAge());
        }
        return userDao.selectList(qp).execute();
    }

    /**
     * 审查请求参数是否合法。
     */
    private String reviewRequest(UserQueryRequest request) {
        boolean hasValidCondition = false;
        StringBuilder reasons = new StringBuilder();
        if (request.name() != null && !request.name().trim().isEmpty()) {
            if (request.name().trim().length() >= 2) {
                hasValidCondition = true;
            } else {
                reasons.append("姓名查询条件至少需要 2 个字符; ");
            }
        }
        if (request.minAge() != null || request.maxAge() != null) {
            hasValidCondition = true;
            if (request.minAge() != null && request.maxAge() != null && request.minAge() > request.maxAge()) {
                return "最小年龄不能大于最大年龄";
            }
        }
        if (!hasValidCondition) {
            return "查询条件不足：请提供有效的姓名（至少 2 个字符）或年龄范围; " + reasons;
        }
        return null;
    }
}
