package com.zhizhi.zhizhiaiagent.agent.model;

import cn.hutool.json.JSONUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 超级智能体 SSE 事件工厂，将结构化事件序列化为 JSON 字符串推送给前端。
 */
public final class AgentStreamEvent {

    private AgentStreamEvent() {
    }

    /**
     * 构建「开始思考」事件。
     *
     * @return JSON 事件字符串
     */
    public static String thinkingStart() {
        return toJson("thinking_start", null, null, null, null);
    }

    /**
     * 构建思考过程增量事件。
     *
     * @param step 当前步号
     * @param text 增量文本
     * @return JSON 事件字符串
     */
    public static String thinkingDelta(int step, String text) {
        return toJson("thinking_delta", step, text, null, null);
    }

    /**
     * 构建「思考结束」事件。
     *
     * @param text      可为 null：前端保留已累积内容，仅切换状态
     * @param elapsedMs 思考耗时（毫秒）
     * @return JSON 事件字符串
     */
    public static String thinkingDone(String text, Long elapsedMs) {
        return toJson("thinking_done", null, text, null, elapsedMs);
    }

    /**
     * 构建步骤进度文案（以 thinking_delta 形式推送，便于前端统一处理）。
     *
     * @param step     当前步
     * @param maxSteps 最大步
     * @param phase    阶段描述
     * @return JSON 事件字符串
     */
    public static String thinkingProgress(int step, int maxSteps, String phase) {
        String text = "第 " + step + "/" + maxSteps + " 步：" + phase;
        return toJson("thinking_delta", step, text, null, null);
    }

    /**
     * 构建单个工具执行完成事件。
     *
     * @param step    当前步
     * @param tool    工具名
     * @param summary 用户可读摘要
     * @return JSON 事件字符串
     */
    public static String toolDone(int step, String tool, String summary) {
        return toolDone(step, tool, summary, null);
    }

    /**
     * @param outcome success / rejected / failed / timeout（前端据此更新计划，避免误标完成）
     */
    public static String toolDone(int step, String tool, String summary, String outcome) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "tool_done");
        if (step > 0) {
            payload.put("step", step);
        }
        if (summary != null) {
            payload.put("text", summary);
        }
        if (tool != null) {
            payload.put("tool", tool);
        }
        if (outcome != null) {
            payload.put("outcome", outcome);
        }
        return JSONUtil.toJsonStr(payload);
    }

    /**
     * 构建最终回答事件。
     *
     * @param text 最终回答正文
     * @return JSON 事件字符串
     */
    public static String answerDone(String text) {
        return toJson("answer_done", null, text, null, null);
    }

    /**
     * 构建错误事件。
     *
     * @param message 错误信息
     * @return JSON 事件字符串
     */
    public static String error(String message) {
        return toJson("error", null, message, null, null);
    }

    /**
     * 构建用户取消事件（随后通常还会推送 answer_done「已停止生成」）。
     *
     * @param text 提示文案
     * @return JSON 事件字符串
     */
    public static String cancelled(String text) {
        return toJson("cancelled", null, text != null ? text : "已停止生成", null, null);
    }

    /**
     * 危险工具人机确认：前端弹窗后调用 /hitl/{id}/approve|reject。
     */
    public static String hitlRequired(String approvalId, String tool, String argsSummary, Integer step) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "hitl_required");
        if (step != null) {
            payload.put("step", step);
        }
        payload.put("approvalId", approvalId);
        payload.put("tool", tool);
        payload.put("text", "危险工具「" + tool + "」待确认");
        if (argsSummary != null) {
            payload.put("arguments", argsSummary);
        }
        return JSONUtil.toJsonStr(payload);
    }

    /**
     * Trace 元信息（可选推送给前端展示）。
     */
    public static String traceMeta(String traceId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "trace_meta");
        payload.put("traceId", traceId);
        return JSONUtil.toJsonStr(payload);
    }

    /**
     * 将事件字段组装为 JSON 字符串。
     *
     * @param type      事件类型
     * @param step      步号（可空）
     * @param text      文本（可空）
     * @param tool      工具名（可空）
     * @param elapsedMs 耗时毫秒（可空）
     * @return JSON 字符串
     */
    private static String toJson(String type, Integer step, String text, String tool, Long elapsedMs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        if (step != null) {
            payload.put("step", step);
        }
        if (text != null) {
            payload.put("text", text);
        }
        if (tool != null) {
            payload.put("tool", tool);
        }
        if (elapsedMs != null) {
            payload.put("elapsedMs", elapsedMs);
        }
        return JSONUtil.toJsonStr(payload);
    }
}
