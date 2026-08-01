package com.zhizhi.zhizhiaiagent.persistence.support;

import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 统一生成 / 校验 32 位字符串 ID（UUID 去掉横线）。
 */
public final class IdGenerator {

    public static final int ID_LENGTH = 32;

    private IdGenerator() {
    }

    public static String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static boolean isValidId(String id) {
        return StringUtils.hasText(id) && id.trim().length() == ID_LENGTH;
    }

    /**
     * @throws IllegalArgumentException 为空或长度不是 32
     */
    public static String requireId(String id, String fieldName) {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        String value = id.trim();
        if (value.length() != ID_LENGTH) {
            throw new IllegalArgumentException(fieldName + " 必须为 " + ID_LENGTH + " 位字符串");
        }
        return value;
    }
}
