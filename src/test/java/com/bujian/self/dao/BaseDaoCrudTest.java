package com.bujian.self.dao;

import com.bujian.self.dto.QueryParam;
import com.bujian.self.dto.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
public class BaseDaoCrudTest {

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
    void insertAndSelect() {
        User u = new User(null, "新用户", 18, "new@example.com");
        assertEquals(1, userDao.insert(u));
        assertEquals(9, userDao.selectCount(new QueryParam<User>() {}));
        User inserted = userDao.selectOne(new QueryParam<User>() {}.eq("name", "新用户"));
        assertNotNull(inserted);
        assertEquals(18, inserted.age());
    }

    @Test
    void updateById() {
        User u = new User(1L, "张三改", 26, "zhangsan2@example.com");
        assertEquals(1, userDao.updateById(u));
        User after = userDao.selectById(1L);
        assertEquals("张三改", after.name());
        assertEquals(26, after.age());
    }

    @Test
    void deleteById() {
        assertEquals(1, userDao.deleteById(1L));
        assertNull(userDao.selectById(1L));
    }

    @Test
    void deleteByQueryParam() {
        QueryParam<User> qp = new QueryParam<User>() {}.lt("age", 25);
        assertEquals(1, userDao.delete(qp));
        assertEquals(7, userDao.selectCount(new QueryParam<User>() {}));
    }
}
