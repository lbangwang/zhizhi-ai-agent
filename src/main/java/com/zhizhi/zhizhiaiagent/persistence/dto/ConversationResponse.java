package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.ConversationEntity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 会话响应 DTO。
 */
@Data
@Builder
public class ConversationResponse {
    /** 主键 ID */
    private String id;
    /** 业务会话 ID，与前端 chatId 对齐 */
    private String chatId;
    /** 所属用户 ID */
    private String userId;
    /** Agent 类型，如 SUPER_AGENT */
    private String agentType;
    /** 会话标题 */
    private String title;
    /** 使用的模型名称 */
    private String model;
    /** 1=进行中 0=归档 */
    private Integer status;
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
