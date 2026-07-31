package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.ConversationEntity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationResponse {
    private Long id;
    private String chatId;
    private Long userId;
    private String agentType;
    private String title;
    private String model;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ConversationResponse from(ConversationEntity entity) {
        return ConversationResponse.builder()
                .id(entity.getId())
                .chatId(entity.getChatId())
                .userId(entity.getUserId())
                .agentType(entity.getAgentType())
                .title(entity.getTitle())
                .model(entity.getModel())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
