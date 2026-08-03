package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.ToolAuditLogEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ToolAuditLogResponse {
    private String id;
    private String userId;
    private String chatId;
    private String toolName;
    private String argumentsSummary;
    private String resultSummary;
    private Integer success;
    private Long durationMs;
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
