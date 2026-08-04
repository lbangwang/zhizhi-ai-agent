package com.zhizhi.zhizhiaiagent.agent.hitl;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录本线程内被 HITL 拒绝/超时的工具，供 tool_done 文案使用。
 */
public final class HitlDecisionTracker {

    private static final ThreadLocal<Set<String>> REJECTED = ThreadLocal.withInitial(
            () -> ConcurrentHashMap.newKeySet());

    private HitlDecisionTracker() {
    }

    public static void clear() {
        REJECTED.get().clear();
        REJECTED.remove();
    }

    public static void markRejected(String toolName) {
        if (toolName != null) {
            REJECTED.get().add(toolName);
        }
    }

    public static boolean wasRejected(String toolName) {
        return toolName != null && REJECTED.get().contains(toolName);
    }

    public static Set<String> snapshot() {
        return Collections.unmodifiableSet(REJECTED.get());
    }
}
