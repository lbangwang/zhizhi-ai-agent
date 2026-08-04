package com.zhizhi.zhizhiaiagent.persistence.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhizhi.zhizhiaiagent.persistence.dto.AgentTraceResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.AgentTraceStatsResponse;
import com.zhizhi.zhizhiaiagent.persistence.dto.ApiResult;
import com.zhizhi.zhizhiaiagent.persistence.service.AgentTraceService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "最近 Trace 列表")
    @GetMapping
    public ApiResult<List<AgentTraceResponse>> list(
            @RequestParam(value = "chatId", required = false) String chatId,
            @RequestParam(value = "limit", defaultValue = "30") int limit) {
        return ApiResult.ok(agentTraceService.list(StpUtil.getLoginIdAsString(), chatId, limit));
    }

    @Operation(summary = "Trace 详情")
    @GetMapping("/{traceId}")
    public ApiResult<AgentTraceResponse> get(@PathVariable String traceId) {
        return ApiResult.ok(agentTraceService.get(traceId, StpUtil.getLoginIdAsString()));
    }

    @Operation(summary = "极简统计（Token / 耗时）")
    @GetMapping("/stats/summary")
    public ApiResult<AgentTraceStatsResponse> stats() {
        return ApiResult.ok(agentTraceService.stats(StpUtil.getLoginIdAsString()));
    }
}
