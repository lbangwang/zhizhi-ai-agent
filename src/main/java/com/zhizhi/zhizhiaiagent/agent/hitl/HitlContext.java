package com.zhizhi.zhizhiaiagent.agent.hitl;

import java.util.Set;
import java.util.function.Consumer;

/**
 * HITL 线程上下文：在工具执行线程上挂载 SSE 推送与会话身份。
 */
public final class HitlContext {

    private static final ThreadLocal<State> HOLDER = new ThreadLocal<>();

    private HitlContext() {
    }

    public static void set(String chatId, String userId, Consumer<String> eventSender) {
        HOLDER.set(new State(chatId, userId, eventSender));
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static State get() {
        return HOLDER.get();
    }

    public record State(String chatId, String userId, Consumer<String> eventSender) {
    }

    /** 终端 / 写文件需二次确认 */
    public static final Set<String> DANGEROUS_TOOLS = Set.of(
            "executeTerminalCommand",
            "writeFile"
    );

    public static boolean isDangerous(String toolName) {
        return toolName != null && DANGEROUS_TOOLS.contains(toolName);
    }
}
