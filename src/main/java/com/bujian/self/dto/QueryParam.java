package com.bujian.self.dto;

import com.bujian.self.config.TableInfoHelp;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 参考 MyBatis-Plus 的 {@code QueryWrapper}/{@code LambdaQueryWrapper} 设计的通用查询参数构造器。
 * 以链式 API 拼装 WHERE / ORDER BY 条件，并生成 NamedParameterJdbcTemplate 风格的
 * {@code :param} 占位符 SQL 与对应的 {@link MapSqlParameterSource}。
 *
 * <p>支持两种列引用方式：
 * <ul>
 *   <li>字符串：{@code eq("name", "张三")}（自动将 Java 属性名转为数据库列名，否则当作列名）</li>
 *   <li>Lambda：{@code eq(User::name, "张三")}（通过 SerializedLambda 解析属性名，再转列名）</li>
 * </ul>
 *
 * <p>构造方式：
 * <ul>
 *   <li>{@code new QueryParam<User>(){}}   —— 无参，实体类从泛型父类解析（须以匿名子类形式使用）</li>
 *   <li>{@code new QueryParam<>(User.class)} —— 显式传入实体类</li>
 *   <li>{@code new QueryParam<>(tableInfo)} —— 直接传入已解析的表元信息</li>
 * </ul>
 *
 * @param <T> 实体类型
 */
public class QueryParam<T> {

    private final TableInfo tableInfo;
    private final List<Segment> segments = new ArrayList<>();
    private final List<OrderItem> orders = new ArrayList<>();
    private final Map<String, Object> lastParams = new LinkedHashMap<>();
    private String lastSql = "";
    private int paramIndex = 0;
    private String pendingConnector = "AND";

    /**
     * 无参构造：实体类从泛型父类解析，须以匿名子类形式使用（如 {@code new QueryParam<User>(){}}），
     * 以便运行时通过 {@link Class#getGenericSuperclass()} 取到实际泛型参数。
     */
    public QueryParam() {
        this.tableInfo = TableInfoHelp.parse(resolveEntityClass());
    }

    public QueryParam(Class<T> entityClass) {
        this(TableInfoHelp.parse(entityClass));
    }

    public QueryParam(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
    }

    @SuppressWarnings("unchecked")
    private Class<T> resolveEntityClass() {
        Class<?> current = getClass();
        while (current != null && current != Object.class) {
            Type superClass = current.getGenericSuperclass();
            if (superClass instanceof ParameterizedType pt && pt.getRawType() == QueryParam.class) {
                Type arg = pt.getActualTypeArguments()[0];
                if (arg instanceof Class<?> clazz) {
                    return (Class<T>) clazz;
                }
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException(
                "无法从泛型父类解析实体类型，请使用匿名子类 new QueryParam<T>(){} 或显式传入 Class：" + getClass().getName());
    }

    // ===================== 列名解析 =====================

    private String toColumn(String fieldOrColumn) {
        if (fieldOrColumn == null) {
            return null;
        }
        String col = tableInfo.fieldToColumn().get(fieldOrColumn);
        return col != null ? col : fieldOrColumn;
    }

    private String toColumn(SFunction<T, ?> fn) {
        return toColumn(resolveFieldName(fn));
    }

    private static String resolveFieldName(SFunction<?, ?> fn) {
        try {
            Method method = fn.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            SerializedLambda sl = (SerializedLambda) method.invoke(fn);
            return methodToFieldName(sl.getImplMethodName());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "无法通过 lambda 解析实体属性名，请确认方法引用来自实体的访问器（如 User::name）", e);
        }
    }

    private static String methodToFieldName(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return decapitalize(methodName.substring(3));
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return decapitalize(methodName.substring(2));
        }
        return methodName;
    }

    private static String decapitalize(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    // ===================== 内部数据结构 =====================

    private record Segment(String connector, String sql, Map<String, Object> params) {
    }

    private record OrderItem(String column, boolean asc) {
    }

    private String nextParam() {
        return "qp" + (paramIndex++);
    }

    private QueryParam<T> add(String sql, Map<String, Object> params) {
        String conn = segments.isEmpty() ? null : pendingConnector;
        segments.add(new Segment(conn, sql, params));
        pendingConnector = "AND";
        return this;
    }

    public QueryParam<T> and() {
        pendingConnector = "AND";
        return this;
    }

    public QueryParam<T> or() {
        pendingConnector = "OR";
        return this;
    }

    // ===================== 比较条件 =====================

    public QueryParam<T> eq(String column, Object val) {
        if (val == null) {
            return add(toColumn(column) + " IS NULL", Map.of());
        }
        return cmp(column, "=", val);
    }

    public QueryParam<T> ne(String column, Object val) {
        if (val == null) {
            return add(toColumn(column) + " IS NOT NULL", Map.of());
        }
        return cmp(column, "<>", val);
    }

    public QueryParam<T> gt(String column, Object val) {
        return cmp(column, ">", val);
    }

    public QueryParam<T> ge(String column, Object val) {
        return cmp(column, ">=", val);
    }

    public QueryParam<T> lt(String column, Object val) {
        return cmp(column, "<", val);
    }

    public QueryParam<T> le(String column, Object val) {
        return cmp(column, "<=", val);
    }

    private QueryParam<T> cmp(String column, String op, Object val) {
        String p = nextParam();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(p, val);
        return add(toColumn(column) + " " + op + " :" + p, map);
    }

    // ===================== 模糊匹配 =====================

    public QueryParam<T> like(String column, Object val) {
        return like(column, val, "BOTH");
    }

    public QueryParam<T> notLike(String column, Object val) {
        return like(column, val, "NOT");
    }

    public QueryParam<T> likeLeft(String column, Object val) {
        return like(column, val, "LEFT");
    }

    public QueryParam<T> likeRight(String column, Object val) {
        return like(column, val, "RIGHT");
    }

    private QueryParam<T> like(String column, Object val, String mode) {
        String p = nextParam();
        String pattern;
        if ("LEFT".equals(mode)) {
            pattern = "%" + val;
        } else if ("RIGHT".equals(mode)) {
            pattern = val + "%";
        } else {
            pattern = "%" + val + "%";
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(p, pattern);
        String op = "NOT".equals(mode) ? "NOT LIKE" : "LIKE";
        return add(toColumn(column) + " " + op + " :" + p, map);
    }

    // ===================== 范围 / 集合 =====================

    public QueryParam<T> between(String column, Object lo, Object hi) {
        String p1 = nextParam();
        String p2 = nextParam();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(p1, lo);
        map.put(p2, hi);
        return add(toColumn(column) + " BETWEEN :" + p1 + " AND :" + p2, map);
    }

    public QueryParam<T> notBetween(String column, Object lo, Object hi) {
        String p1 = nextParam();
        String p2 = nextParam();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(p1, lo);
        map.put(p2, hi);
        return add(toColumn(column) + " NOT BETWEEN :" + p1 + " AND :" + p2, map);
    }

    public QueryParam<T> in(String column, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return add("1 = 0", Map.of());
        }
        String p = nextParam();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(p, new ArrayList<>(values));
        return add(toColumn(column) + " IN (:" + p + ")", map);
    }

    public QueryParam<T> notIn(String column, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return add("1 = 1", Map.of());
        }
        String p = nextParam();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(p, new ArrayList<>(values));
        return add(toColumn(column) + " NOT IN (:" + p + ")", map);
    }

    public QueryParam<T> isNull(String column) {
        return add(toColumn(column) + " IS NULL", Map.of());
    }

    public QueryParam<T> isNotNull(String column) {
        return add(toColumn(column) + " IS NOT NULL", Map.of());
    }

    // ===================== 排序 =====================

    public QueryParam<T> orderByAsc(String column) {
        orders.add(new OrderItem(toColumn(column), true));
        return this;
    }

    public QueryParam<T> orderByDesc(String column) {
        orders.add(new OrderItem(toColumn(column), false));
        return this;
    }

    // ===================== 原生拼接（分页等） =====================

    /**
     * 在 SQL 末尾直接拼接原生片段（如分页 {@code "LIMIT 10 OFFSET 20"}）。
     * 注意：该片段会原样拼接，存在 SQL 注入风险，仅应传入受信任的常量。
     */
    public QueryParam<T> last(String sql) {
        this.lastSql = sql == null ? "" : sql;
        return this;
    }

    /**
     * 在 SQL 末尾拼接原生片段，并附带命名参数（片段中可使用 {@code :name} 占位符）。
     */
    public QueryParam<T> last(String sql, Map<String, Object> params) {
        this.lastSql = sql == null ? "" : sql;
        if (params != null) {
            lastParams.putAll(params);
        }
        return this;
    }

    // ===================== 输出 =====================

    public String toWhereSql() {
        StringBuilder sb = new StringBuilder();
        for (Segment s : segments) {
            if (s.connector() == null) {
                sb.append("WHERE ").append(s.sql());
            } else {
                sb.append(" ").append(s.connector()).append(" ").append(s.sql());
            }
        }
        return sb.toString();
    }

    public String toOrderBySql() {
        if (orders.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("ORDER BY ");
        for (int i = 0; i < orders.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            OrderItem o = orders.get(i);
            sb.append(o.column()).append(o.asc() ? " ASC" : " DESC");
        }
        return sb.toString();
    }

    /**
     * 生成完整的条件片段：WHERE ... ORDER BY ... [last]
     */
    public String toSql() {
        String raw = toWhereSql() + " " + toOrderBySql() + " " + lastSql;
        return raw.trim().replaceAll("\\s+", " ");
    }

    public MapSqlParameterSource getParams() {
        MapSqlParameterSource src = new MapSqlParameterSource();
        for (Segment s : segments) {
            s.params().forEach(src::addValue);
        }
        lastParams.forEach(src::addValue);
        return src;
    }

    // ===================== Lambda 重载 =====================

    public QueryParam<T> eq(SFunction<T, ?> fn, Object val) { return eq(toColumn(fn), val); }

    public QueryParam<T> ne(SFunction<T, ?> fn, Object val) { return ne(toColumn(fn), val); }

    public QueryParam<T> gt(SFunction<T, ?> fn, Object val) { return gt(toColumn(fn), val); }

    public QueryParam<T> ge(SFunction<T, ?> fn, Object val) { return ge(toColumn(fn), val); }

    public QueryParam<T> lt(SFunction<T, ?> fn, Object val) { return lt(toColumn(fn), val); }

    public QueryParam<T> le(SFunction<T, ?> fn, Object val) { return le(toColumn(fn), val); }

    public QueryParam<T> like(SFunction<T, ?> fn, Object val) { return like(toColumn(fn), val); }

    public QueryParam<T> notLike(SFunction<T, ?> fn, Object val) { return notLike(toColumn(fn), val); }

    public QueryParam<T> likeLeft(SFunction<T, ?> fn, Object val) { return likeLeft(toColumn(fn), val); }

    public QueryParam<T> likeRight(SFunction<T, ?> fn, Object val) { return likeRight(toColumn(fn), val); }

    public QueryParam<T> between(SFunction<T, ?> fn, Object lo, Object hi) { return between(toColumn(fn), lo, hi); }

    public QueryParam<T> notBetween(SFunction<T, ?> fn, Object lo, Object hi) { return notBetween(toColumn(fn), lo, hi); }

    public QueryParam<T> in(SFunction<T, ?> fn, Collection<?> values) { return in(toColumn(fn), values); }

    public QueryParam<T> notIn(SFunction<T, ?> fn, Collection<?> values) { return notIn(toColumn(fn), values); }

    public QueryParam<T> isNull(SFunction<T, ?> fn) { return isNull(toColumn(fn)); }

    public QueryParam<T> isNotNull(SFunction<T, ?> fn) { return isNotNull(toColumn(fn)); }

    public QueryParam<T> orderByAsc(SFunction<T, ?> fn) { return orderByAsc(toColumn(fn)); }

    public QueryParam<T> orderByDesc(SFunction<T, ?> fn) { return orderByDesc(toColumn(fn)); }
}
