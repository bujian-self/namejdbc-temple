# SQL Capture Demo Project

## 项目说明

本项目演示了如何使用 `NamedParameterJdbcTemplate` 实现可以获取 SQL 并决定是否执行的功能。

## 核心特性

1. **SQL 捕获**: 在执行 SQL 之前先捕获生成的 SQL 语句
2. **SQL 审查**: 基于 `MapSqlParameterSource` 参数进行业务逻辑判断
3. **条件执行**: 根据审查结果决定是否真正执行 SQL

## 技术栈

- Spring Boot 3.2.0
- Spring JDBC
- H2 内存数据库
- Lombok
- Maven

## 项目结构

```
src/main/java/com/bujian/self/
├── DemoApplication.java                    # 启动类
├── config/
│   └── JdbcConfig.java                     # JDBC 配置类
├── controller/
│   └── UserController.java                 # Controller 层
├── dao/
│   └── UserDao.java                        # DAO 层
├── dto/
│   ├── User.java                           # 用户实体
│   ├── UserQueryRequest.java               # 查询请求参数
│   ├── QueryResponse.java                  # 查询响应
│   ├── SqlCaptureExecutor.java             # SQL 执行封装类
│   └── UserRowMapper.java                  # 行映射器
├── service/
│   ├── UserService.java                    # 服务层（包含 SQL 审查逻辑）
│   └── SqlCapturingNamedParameterJdbcTemplate.java  # 扩展的 JDBC 模板

src/main/resources/
├── application.properties                  # 应用配置
├── schema.sql                              # 数据库表结构
└── data.sql                                # 测试数据
```

## API 接口

### 1. POST /api/users/search

**请求体:**
```json
{
    "name": "张",
    "minAge": 20,
    "maxAge": 40
}
```

**成功响应:**
```json
{
    "success": true,
    "message": "查询成功",
    "data": [
        {
            "id": 1,
            "name": "张三",
            "age": 25,
            "email": "zhangsan@example.com"
        }
    ],
    "sql": "SQL: SELECT id, name, age, email FROM users WHERE 1=1 AND name LIKE :name AND age >= :minAge AND age <= :maxAge\nParameters: ..."
}
```

**失败响应:**
```json
{
    "success": false,
    "message": "查询条件不足：请提供有效的姓名（至少 2 个字符）或年龄范围",
    "data": null,
    "sql": "SQL: ..."
}
```

### 2. GET /api/users/search

**请求参数:**
- name (可选): 姓名模糊匹配
- minAge (可选): 最小年龄
- maxAge (可选): 最大年龄

**示例:**
```
GET /api/users/search?name=张&minAge=20&maxAge=40
```

## SQL 审查规则

当前实现的审查规则：

1. **姓名条件**: 如果提供姓名查询，至少需要 2 个字符
2. **年龄范围**: 如果同时提供最小和最大年龄，最小值不能大于最大值
3. **必要条件**: 必须提供以下任一有效条件：
   - 姓名（至少 2 个字符）
   - 年龄范围（minAge 或 maxAge）

## 运行项目

```bash
# 使用 Maven 运行
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/self-1.0.0-SNAPSHOT.jar
```

## 测试示例

### 1. 有效查询（按姓名）
```bash
curl -X POST http://localhost:8080/api/users/search \
  -H "Content-Type: application/json" \
  -d '{"name": "张三"}'
```

### 2. 有效查询（按年龄范围）
```bash
curl -X POST http://localhost:8080/api/users/search \
  -H "Content-Type: application/json" \
  -d '{"minAge": 25, "maxAge": 35}'
```

### 3. 无效查询（姓名太短）
```bash
curl -X POST http://localhost:8080/api/users/search \
  -H "Content-Type: application/json" \
  -d '{"name": "张"}'
```
响应：`查询条件不足：请提供有效的姓名（至少 2 个字符）或年龄范围`

### 4. 无效查询（年龄范围不合理）
```bash
curl -X POST http://localhost:8080/api/users/search \
  -H "Content-Type: application/json" \
  -d '{"minAge": 40, "maxAge": 20}'
```
响应：`最小年龄不能大于最大年龄`

### 5. 无效查询（无条件）
```bash
curl -X POST http://localhost:8080/api/users/search \
  -H "Content-Type: application/json" \
  -d '{}'
```
响应：`查询条件不足：请提供有效的姓名（至少 2 个字符）或年龄范围`

## 核心设计思路

1. **SqlCaptureExecutor<T>**: 封装 SQL 语句、执行状态、结果和拒绝原因
2. **SqlCapturingNamedParameterJdbcTemplate**: 扩展 JDBC 模板，在真正执行前先进行 SQL 审查
3. **审查函数**: 通过 `Function<MapSqlParameterSource, String>` 传入审查逻辑，在参数层面进行判断
4. **分层架构**: Controller -> Service -> Dao，每层职责清晰

## 注意事项

- 当前 SQL 展示是简化版本，实际项目中可以使用 SQL 日志库生成完整的 SQL
- H2 数据库是内存模式，重启后数据会重置
- 可以通过 `spring.h2.console.enabled=true` 访问 H2 控制台查看数据
