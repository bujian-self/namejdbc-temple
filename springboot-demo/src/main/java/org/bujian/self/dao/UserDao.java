package org.bujian.self.dao;

import org.bujian.dao.BaseDao;
import org.bujian.self.dto.User;
import org.springframework.stereotype.Repository;

/**
 * 用户 DAO，继承通用 {@link BaseDao}，直接复用其查询与 CRUD 能力。
 */
@Repository
public class UserDao extends BaseDao<User> {

    public UserDao() {
        super(User.class);
    }
}
