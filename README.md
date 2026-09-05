# namejdbc-temple
简单的示例

## 项目说明

本项目演示了如何使用 `NamedParameterJdbcTemplate` 实现 SQL 捕获、参数校验与延迟执行的功能：通过 `SqlCaptureExecutor` 封装 SQL、参数与执行函数，由调用方决定何时真正执行查询，并可获取参数替换后的最终 SQL。

## 核心特性

1. **SQL 捕获**: 在真正执行之前，先捕获生成的 SQL 语句与参数
2. **参数校验**: 校验逻辑位于 service 层，基于 `MapSqlParameterSource` 进行业务判断，不通过则拒绝执行
3. **延迟执行**: 执行函数以 `Supplier` / `Runnable` 形式注入，调用方通过 `execute()` 决定执行时机
4. **最终 SQL 展示**: `getSql()` 将命名参数替换为字面量，生成用于展示的最终 SQL

## 技术栈

- Spring Boot 3.4.1
- Java 21
- Spring JDBC（NamedParameterJdbcTemplate）
- H2 内存数据库
- Graceful Response（统一响应封装）
- Maven

## 项目结构

```
namejdbc-plus/                          # 核心库模块
├── pom.xml
└── src/main/java/org/bujian/
    ├── config/
    │   ├── SqlCaptureExecutor.java     # SQL 捕获与执行封装
    │   └── TableInfoHelp.java          # 实体元信息解析工具
    ├── dao/
    │   └── BaseDao.java                # 通用 DAO 基类
    └── dto/
        ├── BaseRowMapper.java          # 通用行映射器
        ├── QueryParam.java             # 查询条件构造器
        ├── SFunction.java              # 可序列化函数式接口
        ├── TableInfo.java              # 表元信息 record
        ├── User.java                   # 用户实体
        └── UserQueryRequest.java       # 查询请求参数

springboot-demo/                        # Spring Boot 应用模块
├── pom.xml
└── src/main/java/org/bujian/self/
    ├── BujianApplication.java          # 启动类
    ├── config/
    │   ├── GlobalExceptionAdvice.java  # 全局异常处理
    │   └── JdbcConfig.java             # JDBC 配置类
    ├── controller/
    │   └── UserController.java         # Controller 层
    ├── dao/
    │   └── UserDao.java                # DAO 层
    ├── dto/
    │   └── User.java                   # 用户实体
    └── service/
        └── UserService.java            # 服务层（参数校验逻辑）
└── src/main/resources/
    ├── application.yml                 # 应用配置
    ├── schema.sql                      # 数据库表结构
    └── data.sql                        # 测试数据
```

## 核心设计思路

1. **SqlCaptureExecutor\<R\>**: 泛型 record，核心组件为 `sql`、`params`、`Supplier<R> queryFunction`
   - 执行函数不接收入参，所需上下文由 lambda 闭包捕获
   - 有返回值场景使用 `Supplier<R>`，无返回值场景（INSERT/UPDATE/DELETE）使用 `Runnable`
   - 提供 `String` 与 `StringBuilder`（sqlbuild）两组构造函数重载
   - `execute()` 触发执行；`getSql()` 返回参数替换后的最终 SQL（仅供展示）
2. **参数校验在 service 层**: `UserService` 自行调用 `reviewSqlParams` 校验，不通过时抛出异常
3. **分层架构**: Controller -> Service -> Dao，每层职责清晰

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

### 2. GET /api/users/search

**请求参数:**
- name (可选): 姓名模糊匹配
- minAge (可选): 最小年龄
- maxAge (可选): 最大年龄

**示例:**
```
GET /api/users/search?name=张&minAge=20&maxAge=40
```

响应由 Graceful Response 统一封装；校验不通过时抛出 `IllegalArgumentException`，由全局异常处理器统一返回。

## 参数校验规则

当前实现的校验规则：

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
java -jar target/springboot-demo-1.0.0-SNAPSHOT.jar
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

## 注意事项

- `getSql()` 生成的 SQL 仅用于展示，不参与实际执行
- H2 数据库是内存模式，重启后数据会重置
- 可以通过 `spring.h2.console.enabled=true` 访问 H2 控制台查看数据
