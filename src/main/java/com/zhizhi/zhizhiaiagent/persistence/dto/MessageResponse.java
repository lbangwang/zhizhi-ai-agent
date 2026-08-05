package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.MessageEntity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 消息响应 DTO。
 */
@Data
@Builder
public class MessageResponse {
    /** 主键 ID */
    private String id;
    /** 所属会话 ID */
    private String conversationId;
    /** 消息角色：user / assistant / system / tool */
    private String role;
    /** 消息正文内容 */
    private String content;
    /** 扩展元数据（JSON 字符串） */
    private String metadata;
    /** 企业/租户 ID */
    private String enterpriseId;
    /** 创建人标识 */
    private String createBy;
    /** 创建时间 */
    private Date createDate;
    /** 最后更新人标识 */
    private String updateBy;
    /** 最后更新时间 */
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
