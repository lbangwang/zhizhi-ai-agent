package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentTraceStatsResponse {
    private long totalRuns;
    private long successRuns;
    private long cancelledRuns;
    private long errorRuns;
    private long totalPromptTokens;
    private long totalCompletionTokens;
    private long totalTokens;
    private long avgDurationMs;
}
