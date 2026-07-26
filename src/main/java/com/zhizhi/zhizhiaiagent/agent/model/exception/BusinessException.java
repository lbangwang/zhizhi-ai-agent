package com.zhizhi.zhizhiaiagent.agent.model.exception;

/**
 * 业务异常
 */
public class BusinessException extends BaseException {

    public BusinessException(String message, String errorCode) {
        super(message, errorCode);
    }
}
