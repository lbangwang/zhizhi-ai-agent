package com.zhizhi.zhizhiaiagent.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户注册请求。
 */
@Data
@Schema(description = "用户注册请求")
public class RegisterRequest {

    @Schema(description = "登录用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
    private String username;

    @Schema(description = "登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "password123")
    private String password;

    @Schema(description = "用户昵称", example = "张三")
    private String nickname;
}
