package com.zhizhi.zhizhiaiagent.agent.model.enums;

/**
 * 通用枚举接口，统一编码和描述
 */
public interface BaseEnum<T> {
    T getCode();      // 获取编码（可以是 Integer 或 String）
    String getDesc(); // 获取描述信息
}