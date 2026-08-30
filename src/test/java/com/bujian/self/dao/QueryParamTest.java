package com.bujian.self.dao;

import com.bujian.self.dto.QueryParam;
import com.bujian.self.dto.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class QueryParamTest {

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
    void countAll() {
        assertEquals(8L, userDao.selectCount(new QueryParam<User>() {}).execute());
    }

    @Test
    void eqLambda() {
        QueryParam<User> qp = new QueryParam<User>() {}.eq(User::name, "张三");
        List<User> list = userDao.selectList(qp).execute();
        assertEquals(1, list.size());
        assertEquals(25, list.get(0).age());
    }

    @Test
    void geString() {
        assertEquals(5L, userDao.selectCount(new QueryParam<User>() {}.ge("age", 30)).execute());
    }

    @Test
    void like() {
        assertEquals(1L, userDao.selectCount(new QueryParam<User>() {}.like("name", "李")).execute());
    }

    @Test
    void between() {
        assertEquals(4L, userDao.selectCount(new QueryParam<User>() {}.between("age", 28, 35)).execute());
    }

    @Test
    void inLambda() {
        QueryParam<User> qp = new QueryParam<User>() {}.in(User::name, List.of("张三", "李四"));
        assertEquals(2, userDao.selectList(qp).execute().size());
    }

    @Test
    void orderByAndLast() {
        QueryParam<User> qp = new QueryParam<User>() {}.orderByDesc("age").last("LIMIT 3");
        List<User> list = userDao.selectList(qp).execute();
        assertEquals(3, list.size());
        assertEquals(45, list.get(0).age());
    }

    @Test
    void orCondition() {
        QueryParam<User> qp = new QueryParam<User>() {}.ge("age", 40).or().like("name", "张");
        assertEquals(3, userDao.selectList(qp).execute().size());
    }

    @Test
    void eqNullToIsNull() {
        QueryParam<User> qp = new QueryParam<User>() {}.eq("email", null);
        assertTrue(qp.toSql().contains("WHERE email IS NULL"));
        assertEquals(0L, userDao.selectCount(qp).execute());
    }

    @Test
    void neNullToIsNotNull() {
        QueryParam<User> qp = new QueryParam<User>() {}.ne("email", null);
        assertTrue(qp.toSql().contains("WHERE email IS NOT NULL"));
        assertEquals(8L, userDao.selectCount(qp).execute());
    }

    @Test
    void sqlContent() {
        QueryParam<User> qp = new QueryParam<User>() {}.eq(User::age, 25);
        assertTrue(qp.toSql().contains("WHERE age = :qp0"));
    }
}
