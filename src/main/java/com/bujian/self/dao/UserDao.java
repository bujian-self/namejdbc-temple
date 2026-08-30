package com.bujian.self.dao;

import org.springframework.stereotype.Repository;

import com.bujian.self.dto.User;

/**
 * 用户 DAO，继承通用 {@link BaseDao}，直接复用其查询与 CRUD 能力。
 */
@Repository
public class UserDao extends BaseDao<User> {

    public UserDao() {
        super(User.class);
    }
}
