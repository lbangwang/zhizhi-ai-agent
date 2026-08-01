package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Data;

@Data
public class CreateConversationRequest {
    /** 32 位字符串；不传则服务端生成 */
    private String chatId;
    /** 用户主键，32 位字符串 */
    private String userId;
    /** 默认 SUPER_AGENT */
    private String agentType;
    private String title;
    private String model;
    /** 32 位字符串，可选，无默认值 */
    private String enterpriseId;
    private String createBy;
}
