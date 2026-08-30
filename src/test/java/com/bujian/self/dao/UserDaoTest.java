package com.bujian.self.dao;

import com.bujian.self.dto.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class UserDaoTest {

    @Autowired
    private UserDao userDao;

    @Test
    void testNoArgConstructorWithResourceInjection() {
        User u = userDao.selectById(1L);
        assertTrue(u != null && "张三".equals(u.name()));
    }

    @Test
    void testSelectListNullParams() {
        List<User> all = userDao.selectList("SELECT * FROM users", null);
        assertFalse(all.isEmpty());
    }
}
