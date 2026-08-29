package com.bujian.self.config;

import com.feiniaojin.gracefulresponse.api.ResponseFactory;
import com.feiniaojin.gracefulresponse.data.Response;
import com.feiniaojin.gracefulresponse.data.ResponseStatus;
import com.feiniaojin.gracefulresponse.defaults.DefaultResponseStatus;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，使用 Graceful Response 进行统一返回封装
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionAdvice {

    private final ResponseFactory responseFactory;

    public GlobalExceptionAdvice(ResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    /**
     * 处理参数校验失败抛出的 IllegalArgumentException，统一返回业务错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Response handleIllegalArgument(IllegalArgumentException ex) {
        ResponseStatus status = new DefaultResponseStatus("400", ex.getMessage());
        return responseFactory.newInstance(status);
    }
}
