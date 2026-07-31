package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Data;

@Data
public class CreateMessageRequest {
    /** user / assistant / system / tool */
    private String role;
    private String content;
    private String metadata;
}
