package org.bujian.dto;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 可序列化函数式接口，用于以 lambda 方式引用实体属性（如 {@code User::name}）。
 * 通过 {@link java.lang.invoke.SerializedLambda} 解析出属性名，再配合 {@link TableInfo} 取得列名。
 *
 * @param <T> 实体类型
 * @param <R> 属性类型
 */
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {
}
