package com.zhizhi.zhizhiaiagent.model;

import java.util.Locale;

/**
 * 前端传入的 model 参数与后端 ChatModel 映射。
 */
public enum ChatModelType {
    DEEPSEEK("deepseek"),
    QWEN("qwen"),
    DOUBAO("doubao");

    private final String code;

    ChatModelType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ChatModelType from(String value) {
        if (value == null || value.isBlank()) {
            return QWEN;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ChatModelType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的模型: " + value + "，可选: deepseek / qwen / doubao");
    }
}
