package com.zhizhi.zhizhiaiagent.persistence.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建消息请求。
 */
@Data
@Schema(description = "创建消息请求")
public class CreateMessageRequest {

    @Schema(description = "消息角色：user / assistant / system / tool",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "user")
    private String role;

    @Schema(description = "消息正文内容", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "请帮我总结一下这份文档")
    private String content;

    @Schema(description = "扩展元数据（JSON 字符串，如思考链、工具调用摘要）")
    private String metadata;

    @Schema(description = "企业 ID，32 位字符串，不传则继承会话的 enterpriseId")
    private String enterpriseId;

    @Schema(description = "创建人标识（服务端自动填充）", accessMode = Schema.AccessMode.READ_ONLY)
    private String createBy;
}
