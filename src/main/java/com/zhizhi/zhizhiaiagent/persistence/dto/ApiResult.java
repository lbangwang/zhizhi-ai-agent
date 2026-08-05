package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应包装。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    /** 业务状态码，0 表示成功 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 响应数据载荷 */
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
