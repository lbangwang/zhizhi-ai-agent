package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Data;

@Data
public class CreateMessageRequest {
    /** user / assistant / system / tool */
    private String role;
    private String content;
    private String metadata;
    /** 32 位字符串，不传则继承会话的 enterpriseId */
    private String enterpriseId;
    private String createBy;
}
