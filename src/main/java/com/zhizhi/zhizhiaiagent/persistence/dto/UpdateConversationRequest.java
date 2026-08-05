package com.zhizhi.zhizhiaiagent.persistence.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新会话请求。
 */
@Data
@Schema(description = "更新会话请求")
public class UpdateConversationRequest {

    @Schema(description = "会话标题", example = "项目讨论")
    private String title;

    @Schema(description = "使用的模型名称", example = "qwen")
    private String model;

    @Schema(description = "会话状态：1=进行中，0=归档", example = "1")
    private Integer status;
}
