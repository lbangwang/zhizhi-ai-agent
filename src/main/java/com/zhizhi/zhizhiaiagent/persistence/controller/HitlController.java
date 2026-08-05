package com.zhizhi.zhizhiaiagent.persistence.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhizhi.zhizhiaiagent.agent.hitl.HitlApprovalService;
import com.zhizhi.zhizhiaiagent.persistence.dto.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "人机确认 HITL")
@RestController
@RequestMapping("/hitl")
@RequiredArgsConstructor
public class HitlController {

    private final HitlApprovalService hitlApprovalService;

    @Operation(summary = "允许执行危险工具",
            description = "用户确认批准 HITL 审批单，允许 Agent 继续执行危险工具调用。")
    @PostMapping("/{approvalId}/approve")
    public ApiResult<Map<String, Object>> approve(
            @Parameter(description = "HITL 审批单 ID", required = true, example = "hitl-20250805143000-abc123")
            @PathVariable String approvalId) {
        boolean ok = hitlApprovalService.approve(approvalId, StpUtil.getLoginIdAsString());
        if (!ok) {
            throw new IllegalArgumentException("审批单不存在或已失效");
        }
        return ApiResult.ok(Map.of("approvalId", approvalId, "decision", "APPROVED"));
    }

    @Operation(summary = "拒绝执行危险工具",
            description = "用户拒绝 HITL 审批单，阻止 Agent 执行对应的危险工具调用。")
    @PostMapping("/{approvalId}/reject")
    public ApiResult<Map<String, Object>> reject(
            @Parameter(description = "HITL 审批单 ID", required = true, example = "hitl-20250805143000-abc123")
            @PathVariable String approvalId) {
        boolean ok = hitlApprovalService.reject(approvalId, StpUtil.getLoginIdAsString());
        if (!ok) {
            throw new IllegalArgumentException("审批单不存在或已失效");
        }
        return ApiResult.ok(Map.of("approvalId", approvalId, "decision", "REJECTED"));
    }
}
