package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Data;

@Data
public class CreateConversationRequest {
    /** 不传则服务端生成 */
    private String chatId;
    private Long userId;
    /** 默认 SUPER_AGENT */
    private String agentType;
    private String title;
    private String model;
}
