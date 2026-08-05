package com.zhizhi.zhizhiaiagent.persistence.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Agent Trace 聚合统计响应。
 */
@Data
@Builder
public class AgentTraceStatsResponse {
    /** 总运行次数 */
    private long totalRuns;
    /** 成功运行次数 */
    private long successRuns;
    /** 取消运行次数 */
    private long cancelledRuns;
    /** 失败运行次数 */
    private long errorRuns;
    /** 累计提示词 Token 数 */
    private long totalPromptTokens;
    /** 累计补全 Token 数 */
    private long totalCompletionTokens;
    /** 累计总 Token 数 */
    private long totalTokens;
    /** 平均运行耗时（毫秒） */
    private long avgDurationMs;
}
