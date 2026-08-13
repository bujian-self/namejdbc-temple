package com.bujian.self.config;

import com.feiniaojin.gracefulresponse.advice.DefaultGlobalExceptionAdvice;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，使用 Graceful Response 进行统一返回封装
 */
@RestControllerAdvice
@Order(-1)
public class GlobalExceptionAdvice extends DefaultGlobalExceptionAdvice {
}
