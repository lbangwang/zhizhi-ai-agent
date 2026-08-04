package com.zhizhi.zhizhiaiagent.agent.hitl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 危险工具人机确认：内存 Future 等待前端 approve/reject。
 */
@Slf4j
@Service
public class HitlApprovalService {

    public enum Decision {
        APPROVED,
        REJECTED,
        TIMEOUT
    }

    public record PendingApproval(
            String approvalId,
            String chatId,
            String userId,
            String toolName,
            String argumentsSummary,
            long createdAtMs
    ) {
    }

    private static final long DEFAULT_TIMEOUT_SEC = 120;

    private final Map<String, CompletableFuture<Decision>> futures = new ConcurrentHashMap<>();
    private final Map<String, PendingApproval> pending = new ConcurrentHashMap<>();

    public Decision requestAndWait(
            String approvalId,
            String chatId,
            String userId,
            String toolName,
            String argumentsSummary,
            long timeoutSec) {
        CompletableFuture<Decision> future = new CompletableFuture<>();
        PendingApproval meta = new PendingApproval(
                approvalId, chatId, userId, toolName, argumentsSummary, System.currentTimeMillis());
        futures.put(approvalId, future);
        pending.put(approvalId, meta);
        try {
            long wait = timeoutSec > 0 ? timeoutSec : DEFAULT_TIMEOUT_SEC;
            return future.get(wait, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("HITL timeout approvalId={}, tool={}", approvalId, toolName);
            complete(approvalId, Decision.TIMEOUT);
            return Decision.TIMEOUT;
        } catch (Exception e) {
            log.warn("HITL wait failed approvalId={}: {}", approvalId, e.getMessage());
            complete(approvalId, Decision.REJECTED);
            return Decision.REJECTED;
        } finally {
            futures.remove(approvalId);
            pending.remove(approvalId);
        }
    }

    public boolean approve(String approvalId, String userId) {
        return decide(approvalId, userId, Decision.APPROVED);
    }

    public boolean reject(String approvalId, String userId) {
        return decide(approvalId, userId, Decision.REJECTED);
    }

    public PendingApproval getPending(String approvalId) {
        return pending.get(approvalId);
    }

    private boolean decide(String approvalId, String userId, Decision decision) {
        PendingApproval meta = pending.get(approvalId);
        if (meta == null) {
            return false;
        }
        if (userId != null && meta.userId() != null && !userId.equals(meta.userId())) {
            return false;
        }
        return complete(approvalId, decision);
    }

    private boolean complete(String approvalId, Decision decision) {
        CompletableFuture<Decision> future = futures.get(approvalId);
        if (future == null) {
            return false;
        }
        return future.complete(decision);
    }
}
