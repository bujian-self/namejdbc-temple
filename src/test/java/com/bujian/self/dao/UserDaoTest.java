package com.bujian.self.dao;

import com.bujian.self.dto.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class UserDaoTest {

    @Autowired
    private UserDao userDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("INSERT INTO users (id, name, age, email) VALUES " +
                "(1, '张三', 25, 'zhangsan@example.com')," +
                "(2, '李四', 30, 'lisi@example.com')," +
                "(3, '王五', 35, 'wangwu@example.com')," +
                "(4, '赵六', 28, 'zhaoliu@example.com')," +
                "(5, '孙七', 32, 'sunqi@example.com')," +
                "(6, '周八', 40, 'zhouba@example.com')," +
                "(7, '吴九', 22, 'wujiu@example.com')," +
                "(8, '郑十', 45, 'zhengshi@example.com')");
    }

    @Test
    void testNoArgConstructorWithResourceInjection() {
        User u = userDao.selectById(1L);
        assertTrue(u != null && "张三".equals(u.name()));
    }
}
