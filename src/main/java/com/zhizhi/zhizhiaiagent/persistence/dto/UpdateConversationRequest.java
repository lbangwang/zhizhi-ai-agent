package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Data;

@Data
public class UpdateConversationRequest {
    private String title;
    private String model;
    /** 1=进行中 0=归档 */
    private Integer status;
}
