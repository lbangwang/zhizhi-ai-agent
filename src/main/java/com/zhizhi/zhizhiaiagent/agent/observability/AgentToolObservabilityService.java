package com.zhizhi.zhizhiaiagent.agent.observability;

import com.zhizhi.zhizhiaiagent.persistence.service.ArtifactService;
import com.zhizhi.zhizhiaiagent.persistence.service.ToolAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 工具执行后的副作用：审计落库 + 产物入库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.datasource", name = "enabled", havingValue = "true")
public class AgentToolObservabilityService {

    private final ToolAuditService toolAuditService;
    private final ArtifactService artifactService;

    public void afterToolBatch(
            String userId,
            String chatId,
            List<AssistantMessage.ToolCall> toolCalls,
            List<ToolResponseMessage.ToolResponse> responses,
            long durationMs) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        Map<String, String> argsByName = new HashMap<>();
        if (toolCalls != null) {
            for (AssistantMessage.ToolCall call : toolCalls) {
                if (call != null && call.name() != null) {
                    argsByName.put(call.name(), call.arguments());
                }
            }
        }

        int n = responses.size();
        for (int i = 0; i < n; i++) {
            ToolResponseMessage.ToolResponse response = responses.get(i);
            String toolName = response.name();
            String args;
            if (toolCalls != null && i < toolCalls.size()
                    && toolCalls.get(i) != null
                    && toolName != null
                    && toolName.equals(toolCalls.get(i).name())) {
                args = toolCalls.get(i).arguments();
            } else {
                args = argsByName.get(toolName);
            }
            String result = response.responseData();
            try {
                toolAuditService.record(userId, chatId, toolName, args, result, durationMs);
            } catch (Exception e) {
                log.warn("tool audit failed tool={}: {}", toolName, e.getMessage());
            }
            try {
                artifactService.tryRegisterFromToolResult(userId, chatId, toolName, result);
            } catch (Exception e) {
                log.warn("artifact register failed tool={}: {}", toolName, e.getMessage());
            }
        }
    }
}
