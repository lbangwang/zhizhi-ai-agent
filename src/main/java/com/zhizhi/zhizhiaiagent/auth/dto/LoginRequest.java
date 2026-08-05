package com.zhizhi.zhizhiaiagent.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户登录请求。
 */
@Data
@Schema(description = "用户登录请求")
public class LoginRequest {

    @Schema(description = "登录用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
    private String username;

    @Schema(description = "登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "password123")
    private String password;
}
