package com.zhizhi.zhizhiaiagent.persistence.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建用户请求。
 */
@Data
@Schema(description = "创建用户请求")
public class CreateUserRequest {

    @Schema(description = "登录用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
    private String username;

    @Schema(description = "登录密码（明文，服务端存储前需哈希）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "password123")
    private String password;

    @Schema(description = "用户昵称", example = "张三")
    private String nickname;

    @Schema(description = "企业 ID，32 位字符串，可选")
    private String enterpriseId;

    @Schema(description = "创建人标识")
    private String createBy;
}
