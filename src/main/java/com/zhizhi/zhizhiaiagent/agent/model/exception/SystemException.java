package com.zhizhi.zhizhiaiagent.agent.model.exception;

/**
 * 系统异常
 */
public class SystemException extends BaseException {
    public SystemException(String message, String errorCode) {
        super(message, errorCode);
    }

    public SystemException(String message, String errorCode, Throwable cause) {
        super(message, errorCode);
        initCause(cause);
    }
}
