package com.zhizhi.zhizhiaiagent.agent.model.exception;

/**
 * 最佳实践：自定义异常基类 + 具体异常子类
 */
// 1. 基类
public abstract class BaseException extends RuntimeException {
    private final String errorCode;

    public BaseException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}




