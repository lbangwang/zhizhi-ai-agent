package com.zhizhi.zhizhiaiagent.persistence.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建会话请求。
 */
@Data
@Schema(description = "创建会话请求")
public class CreateConversationRequest {

    @Schema(description = "32 位字符串；不传则服务端生成", example = "a1b2c3d4e5f6789012345678abcdef01")
    private String chatId;

    @Schema(description = "用户主键，32 位字符串（服务端自动填充）", accessMode = Schema.AccessMode.READ_ONLY)
    private String userId;

    @Schema(description = "智能体类型，默认 SUPER_AGENT", example = "SUPER_AGENT")
    private String agentType;

    @Schema(description = "会话标题", example = "新对话")
    private String title;

    @Schema(description = "使用的模型名称", example = "qwen")
    private String model;

    @Schema(description = "企业 ID，32 位字符串，可选")
    private String enterpriseId;

    @Schema(description = "创建人标识（服务端自动填充）", accessMode = Schema.AccessMode.READ_ONLY)
    private String createBy;
}
