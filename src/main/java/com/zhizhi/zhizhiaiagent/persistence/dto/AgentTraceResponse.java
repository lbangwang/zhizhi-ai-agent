package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.AgentTraceEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class AgentTraceResponse {
    private String id;
    private String traceId;
    private String userId;
    private String chatId;
    private String agentType;
    private String status;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long durationMs;
    private Integer stepCount;
    private String errorMessage;
    private Date createDate;

    public static AgentTraceResponse from(AgentTraceEntity e) {
        return AgentTraceResponse.builder()
                .id(e.getId())
                .traceId(e.getTraceId())
                .userId(e.getUserId())
                .chatId(e.getChatId())
                .agentType(e.getAgentType())
                .status(e.getStatus())
                .promptTokens(e.getPromptTokens())
                .completionTokens(e.getCompletionTokens())
                .totalTokens(e.getTotalTokens())
                .durationMs(e.getDurationMs())
                .stepCount(e.getStepCount())
                .errorMessage(e.getErrorMessage())
                .createDate(e.getCreateDate())
                .build();
    }
}
