package com.zhizhi.zhizhiaiagent.persistence.controller;

import com.zhizhi.zhizhiaiagent.persistence.dto.ApiResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.zhizhi.zhizhiaiagent.persistence")
public class PersistenceExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResult.fail(ex.getMessage());
    }
}
