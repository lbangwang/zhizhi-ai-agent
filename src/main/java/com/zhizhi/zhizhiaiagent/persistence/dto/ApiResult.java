package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "ok", data);
    }

    public static <T> ApiResult<T> ok(String message, T data) {
        return new ApiResult<>(0, message, data);
    }

    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(-1, message, null);
    }
}
