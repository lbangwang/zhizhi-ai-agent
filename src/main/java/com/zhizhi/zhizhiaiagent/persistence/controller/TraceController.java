package com.zhizhi.zhizhiaiagent.persistence.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhizhi.zhizhiaiagent.persistence.dto.AgentTraceResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.AgentTraceStatsResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.ApiResult;
import com.zhizhi.zhizhiaiagent.persistence.service.AgentTraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Trace 可观测")
@RestController
@RequestMapping("/traces")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.datasource", name = "enabled", havingValue = "true")
public class TraceController {

    private final AgentTraceService agentTraceService;

    @Operation(summary = "最近 Trace 列表",
            description = "查询当前用户最近的 Agent 执行链路记录，可按会话筛选。")
    @GetMapping
    public ApiResult<List<AgentTraceResponse>> list(
            @Parameter(description = "会话 ID，不传则返回全部会话的 Trace", required = false,
                    example = "a1b2c3d4e5f6789012345678abcdef01")
            @RequestParam(value = "chatId", required = false) String chatId,
            @Parameter(description = "返回条数上限", required = false, example = "30")
            @RequestParam(value = "limit", defaultValue = "30") int limit) {
        return ApiResult.ok(agentTraceService.list(StpUtil.getLoginIdAsString(), chatId, limit));
    }

    @Operation(summary = "Trace 详情", description = "按 Trace ID 查询单条 Agent 执行链路的完整详情。")
    @GetMapping("/{traceId}")
    public ApiResult<AgentTraceResponse> get(
            @Parameter(description = "Trace 链路 ID", required = true, example = "trace-20250805143000-abc123")
            @PathVariable String traceId) {
        return ApiResult.ok(agentTraceService.get(traceId, StpUtil.getLoginIdAsString()));
    }

    @Operation(summary = "极简统计（Token / 耗时）",
            description = "汇总当前用户的 Token 消耗与平均耗时等可观测指标。")
    @GetMapping("/stats/summary")
    public ApiResult<AgentTraceStatsResponse> stats() {
        return ApiResult.ok(agentTraceService.stats(StpUtil.getLoginIdAsString()));
    }
}
