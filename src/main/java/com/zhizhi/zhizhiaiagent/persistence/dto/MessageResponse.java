package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.MessageEntity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private String metadata;
    private LocalDateTime createdAt;

    public static MessageResponse from(MessageEntity entity) {
        return MessageResponse.builder()
                .id(entity.getId())
                .conversationId(entity.getConversationId())
                .role(entity.getRole())
                .content(entity.getContent())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
