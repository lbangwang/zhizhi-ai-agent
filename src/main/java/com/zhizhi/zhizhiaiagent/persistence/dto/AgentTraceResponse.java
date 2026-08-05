package com.zhizhi.zhizhiaiagent.persistence.dto;

import com.zhizhi.zhizhiaiagent.persistence.entity.AgentTraceEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * Agent 运行 Trace 响应 DTO。
 */
@Data
@Builder
public class AgentTraceResponse {
    /** 主键 ID */
    private String id;
    /** Trace 唯一标识 */
    private String traceId;
    /** 所属用户 ID */
    private String userId;
    /** 关联会话 chatId */
    private String chatId;
    /** Agent 类型 */
    private String agentType;
    /** 运行状态：RUNNING / SUCCESS / CANCELLED / ERROR */
    private String status;
    /** 提示词 Token 数 */
    private Integer promptTokens;
    /** 补全 Token 数 */
    private Integer completionTokens;
    /** 总 Token 数 */
    private Integer totalTokens;
    /** 运行耗时（毫秒） */
    private Long durationMs;
    /** Agent 执行步数 */
    private Integer stepCount;
    /** 失败时的错误信息 */
    private String errorMessage;
    /** 创建时间 */
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
