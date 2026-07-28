package com.zhizhi.zhizhiaiagent.agent.model;

import cn.hutool.json.JSONUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 超级智能体 SSE 事件：thinking_* / answer_* / error
 */
public final class AgentStreamEvent {

    private AgentStreamEvent() {
    }

    public static String thinkingStart() {
        return of("thinking_start", null, null, null);
    }

    public static String thinkingDelta(int step, String text) {
        return of("thinking_delta", step, text, null);
    }

    /**
     * @param text 可为 null：前端保留已累积的思考内容，仅切换为「已完成」状态
     */
    public static String thinkingDone(String text) {
        return of("thinking_done", null, text, null);
    }

    public static String thinkingProgress(int step, int maxSteps, String phase) {
        String text = "▶ 第 " + step + "/" + maxSteps + " 步：" + phase;
        return of("thinking_delta", step, text, null);
    }

    public static String answerDone(String text) {
        return of("answer_done", null, text, null);
    }

    public static String error(String message) {
        return of("error", null, message, null);
    }

    private static String of(String type, Integer step, String text, String tool) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        if (step != null) {
            map.put("step", step);
        }
        if (text != null) {
            map.put("text", text);
        }
        if (tool != null) {
            map.put("tool", tool);
        }
        return JSONUtil.toJsonStr(map);
    }
}
