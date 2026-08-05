package com.zhizhi.zhizhiaiagent.auth.controller;

import com.zhizhi.zhizhiaiagent.persistence.dto.ApiResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//当且仅当配置了数据源时，才注入bean
@RestControllerAdvice(basePackages = "com.zhizhi.zhizhiaiagent.auth")
@ConditionalOnProperty(prefix = "app.datasource", name = "enabled", havingValue = "true")
public class AuthExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResult.fail(ex.getMessage());
    }
}
