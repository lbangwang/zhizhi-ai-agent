package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.MessageEntity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
public class MessageResponse {
    private String id;
    private String conversationId;
    private String role;
    private String content;
    private String metadata;
    private String enterpriseId;
    private String createBy;
    private Date createDate;
    private String updateBy;
    private Date updateDate;

    public static MessageResponse from(MessageEntity entity) {
        return MessageResponse.builder()
                .id(entity.getId())
                .conversationId(entity.getConversationId())
                .role(entity.getRole())
                .content(entity.getContent())
                .metadata(entity.getMetadata())
                .enterpriseId(entity.getEnterpriseId())
                .createBy(entity.getCreateBy())
                .createDate(entity.getCreateDate())
                .updateBy(entity.getUpdateBy())
                .updateDate(entity.getUpdateDate())
                .build();
    }
}
