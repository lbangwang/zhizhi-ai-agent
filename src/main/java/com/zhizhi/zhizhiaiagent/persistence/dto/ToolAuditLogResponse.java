package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.ToolAuditLogEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 工具调用审计日志响应 DTO。
 */
@Data
@Builder
public class ToolAuditLogResponse {
    /** 主键 ID */
    private String id;
    /** 调用用户 ID */
    private String userId;
    /** 关联会话 chatId */
    private String chatId;
    /** 工具名称 */
    private String toolName;
    /** 入参摘要 */
    private String argumentsSummary;
    /** 返回结果摘要 */
    private String resultSummary;
    /** 1=成功 0=失败 */
    private Integer success;
    /** 调用耗时（毫秒） */
    private Long durationMs;
    /** 创建时间 */
    private Date createDate;

    public static ToolAuditLogResponse from(ToolAuditLogEntity entity) {
        return ToolAuditLogResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .chatId(entity.getChatId())
                .toolName(entity.getToolName())
                .argumentsSummary(entity.getArgumentsSummary())
                .resultSummary(entity.getResultSummary())
                .success(entity.getSuccess())
                .durationMs(entity.getDurationMs())
                .createDate(entity.getCreateDate())
                .build();
    }
}
