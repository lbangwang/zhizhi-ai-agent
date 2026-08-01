package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.ConversationEntity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
public class ConversationResponse {
    private String id;
    private String chatId;
    private String userId;
    private String agentType;
    private String title;
    private String model;
    private Integer status;
    private String enterpriseId;
    private String createBy;
    private Date createDate;
    private String updateBy;
    private Date updateDate;

    public static ConversationResponse from(ConversationEntity entity) {
        return ConversationResponse.builder()
                .id(entity.getId())
                .chatId(entity.getChatId())
                .userId(entity.getUserId())
                .agentType(entity.getAgentType())
                .title(entity.getTitle())
                .model(entity.getModel())
                .status(entity.getStatus())
                .enterpriseId(entity.getEnterpriseId())
                .createBy(entity.getCreateBy())
                .createDate(entity.getCreateDate())
                .updateBy(entity.getUpdateBy())
                .updateDate(entity.getUpdateDate())
                .build();
    }
}
