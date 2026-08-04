package com.zhizhi.zhizhiaiagent.agent.hitl;

import com.zhizhi.zhizhiaiagent.agent.model.AgentStreamEvent;
import com.zhizhi.zhizhiaiagent.persistence.support.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.lang.Nullable;

/**
 * 包装危险工具：执行前推送 hitl_required，阻塞等待用户允许/拒绝。
 */
@Slf4j
@RequiredArgsConstructor
public class HitlGuardedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final HitlApprovalService hitlApprovalService;
    private final long timeoutSec;

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        String toolName = getToolDefinition().name();
        if (!HitlContext.isDangerous(toolName)) {
            return invokeDelegate(toolInput, toolContext);
        }

        HitlContext.State state = HitlContext.get();
        if (state == null || state.eventSender() == null) {
            // 非流式 / 无上下文时默认拒绝，避免静默执行危险操作
            log.warn("HITL context missing, reject tool={}", toolName);
            return "Error: 危险工具需要人机确认，但当前无 HITL 上下文，已拒绝执行。";
        }

        String approvalId = IdGenerator.nextId();
        String argsSummary = truncate(toolInput, 500);
        try {
            state.eventSender().accept(AgentStreamEvent.hitlRequired(
                    approvalId, toolName, argsSummary, null));
        } catch (Exception e) {
            log.warn("push hitl_required failed: {}", e.getMessage());
            return "Error: 无法推送人机确认请求，已拒绝执行。";
        }

        HitlApprovalService.Decision decision = hitlApprovalService.requestAndWait(
                approvalId,
                state.chatId(),
                state.userId(),
                toolName,
                argsSummary,
                timeoutSec);

        if (decision == HitlApprovalService.Decision.APPROVED) {
            log.info("HITL approved tool={}, approvalId={}", toolName, approvalId);
            return invokeDelegate(toolInput, toolContext);
        }
        String reason = decision == HitlApprovalService.Decision.TIMEOUT
                ? "确认超时"
                : "用户拒绝";
        HitlDecisionTracker.markRejected(toolName);
        log.info("HITL {} tool={}, approvalId={}", reason, toolName, approvalId);
        return "Error: 危险工具「" + toolName + "」未执行（" + reason + "）。";
    }

    private String invokeDelegate(String toolInput, @Nullable ToolContext toolContext) {
        if (toolContext != null) {
            return delegate.call(toolInput, toolContext);
        }
        return delegate.call(toolInput);
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }
}
