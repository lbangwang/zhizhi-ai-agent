package com.zhizhi.zhizhiaiagent.agent.model;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 用户可见文案格式化工具。
 * <p>
 * 职责：把模型思考文本、工具原始结果、最终回答转成可读内容；
 * 过滤工具 JSON dump 与过程汇报类元叙述，避免污染前端展示。
 */
public final class AgentUserFacingFormatter {

    private static final int MAX_THINK_CHARS = 800;
    private static final int MAX_TOOL_SUMMARY_CHARS = 280;
    private static final int MAX_LIST_ITEMS = 5;

    private static final Map<String, String> TOOL_LABELS = Map.ofEntries(
            Map.entry("searchWeb", "网页搜索"),
            Map.entry("scrapeWebPage", "网页抓取"),
            Map.entry("downloadResource", "资源下载"),
            Map.entry("readFile", "读取文件"),
            Map.entry("writeFile", "写入文件"),
            Map.entry("executeTerminalCommand", "终端命令"),
            Map.entry("generatePDF", "生成 PDF"),
            Map.entry("doTerminate", "结束任务"),
            Map.entry("searchImage", "图片搜索")
    );

    private AgentUserFacingFormatter() {
    }

    /**
     * 将工具英文名映射为中文展示名。
     *
     * @param toolName 工具名
     * @return 中文标签；未知工具则原样返回
     */
    public static String toolLabel(String toolName) {
        if (StringUtils.isBlank(toolName)) {
            return "工具";
        }
        String label = TOOL_LABELS.get(toolName);
        return label != null ? label : toolName;
    }

    /**
     * tool_done 展示文案：区分 HITL 拒绝 / 执行失败 / 成功。
     */
    public static String toolDoneSummary(String toolName, String rawResult) {
        String label = toolLabel(toolName);
        String data = normalizeToolResult(rawResult);
        if (!StringUtils.isNotBlank(data)) {
            // 无结果时不能默认「已完成」（HITL 拒绝场景曾因此误标）
            return label + " 已结束";
        }
        String lower = data.toLowerCase(Locale.ROOT);
        if (data.contains("用户拒绝")
                || (data.contains("未执行") && (data.contains("危险工具") || data.contains("人机确认")))) {
            return label + " 已拒绝";
        }
        if (data.contains("确认超时") || lower.contains("timeout")) {
            return label + " 已超时";
        }
        if (lower.startsWith("error")
                || lower.contains("exception")
                || data.startsWith("Error")
                || data.contains("失败")) {
            return label + " 失败";
        }
        if (lower.contains("successfully") || data.contains("成功")) {
            return label + " 已完成";
        }
        // 有返回但非明确成功：偏保守标为已结束，避免误报成功
        return label + " 已结束";
    }

    public static boolean isHitlRejectedResult(String rawResult) {
        String data = normalizeToolResult(rawResult);
        return StringUtils.isNotBlank(data)
                && (data.contains("用户拒绝")
                || (data.contains("未执行") && data.contains("危险工具")));
    }

    private static String normalizeToolResult(String rawResult) {
        if (!StringUtils.isNotBlank(rawResult)) {
            return "";
        }
        String data = rawResult.trim();
        // Spring AI 结果可能带 JSON 字符串引号
        while (data.length() >= 2 && data.startsWith("\"") && data.endsWith("\"")) {
            data = data.substring(1, data.length() - 1).trim();
        }
        return data;
    }

    /**
     * 将模型思考原文转为思考区可读文案（压缩 JSON，截断过长内容）。
     *
     * @param raw 模型原始思考文本
     * @return 用户可见思考文案
     */
    public static String toThinkingDisplay(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                JSONObject obj = JSONUtil.parseObj(trimmed);
                StringBuilder sb = new StringBuilder();
                String summary = obj.getStr("summary");
                if (StringUtils.isNotBlank(summary)) {
                    sb.append(summary.trim());
                }
                Object steps = obj.get("steps");
                if (steps instanceof JSONArray arr && !arr.isEmpty()) {
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                    int limit = Math.min(arr.size(), MAX_LIST_ITEMS);
                    for (int i = 0; i < limit; i++) {
                        Object item = arr.get(i);
                        String line = stringifyStep(item);
                        if (StringUtils.isNotBlank(line)) {
                            sb.append(i + 1).append(". ").append(line).append('\n');
                        }
                    }
                }
                if (!sb.isEmpty()) {
                    return truncate(sb.toString().trim(), MAX_THINK_CHARS);
                }
            } catch (Exception ignored) {
                // fall through
            }
        }
        return truncate(trimmed, MAX_THINK_CHARS);
    }

    /**
     * 将计划调用的工具名列表格式化为思考区一行摘要。
     *
     * @param toolNames 工具名列表
     * @return 如「准备调用：网页搜索、网页抓取」
     */
    public static String toToolPlanDisplay(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return "";
        }
        List<String> labels = new ArrayList<>();
        for (String name : toolNames) {
            labels.add(toolLabel(name));
        }
        return "准备调用：" + String.join("、", labels);
    }

    /**
     * 将工具原始返回压缩为用户可读摘要（不暴露完整 JSON payload）。
     *
     * @param toolName     工具名
     * @param responseData 工具原始返回
     * @return 摘要文案
     */
    public static String toToolResultDisplay(String toolName, String responseData) {
        String label = toolLabel(toolName);
        if (StringUtils.isBlank(responseData)) {
            return "「" + label + "」已结束，暂无有效结果。";
        }
        String data = normalizeToolResult(responseData);
        // HITL 拒绝 / 超时：绝不能写成「已完成」，否则计划面板会被误导
        if (isHitlRejectedResult(data)) {
            return "「" + label + "」未执行（用户拒绝）。";
        }
        if (data.contains("确认超时") || data.toLowerCase(Locale.ROOT).contains("timeout")) {
            return "「" + label + "」未执行（确认超时）。";
        }
        if (data.startsWith("Error") || data.toLowerCase(Locale.ROOT).startsWith("error")) {
            String plain = data.replaceAll("\\s+", " ").trim();
            if (plain.length() > 120) {
                plain = plain.substring(0, 120) + "…";
            }
            return truncate("「" + label + "」执行失败：" + plain, MAX_TOOL_SUMMARY_CHARS);
        }
        // 常见：searchWeb 返回 JSON 数组字符串
        if (data.startsWith("[") || data.startsWith("{")) {
            try {
                if (data.startsWith("[")) {
                    JSONArray arr = JSONUtil.parseArray(data);
                    int n = arr.size();
                    List<String> titles = new ArrayList<>();
                    for (int i = 0; i < Math.min(n, MAX_LIST_ITEMS); i++) {
                        Object item = arr.get(i);
                        if (item instanceof JSONObject obj) {
                            String title = firstNonBlank(obj.getStr("title"), obj.getStr("name"));
                            if (StringUtils.isNotBlank(title)) {
                                titles.add(title.trim());
                            }
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("已完成「").append(label).append("」，共检索到 ").append(n).append(" 条结果");
                    if (!titles.isEmpty()) {
                        sb.append("，例如：");
                        for (int i = 0; i < titles.size(); i++) {
                            if (i > 0) {
                                sb.append("；");
                            }
                            sb.append(titles.get(i));
                        }
                    }
                    sb.append("。");
                    return truncate(sb.toString(), MAX_TOOL_SUMMARY_CHARS);
                }
                JSONObject obj = JSONUtil.parseObj(data);
                String summary = firstNonBlank(obj.getStr("summary"), obj.getStr("message"), obj.getStr("content"));
                if (StringUtils.isNotBlank(summary)) {
                    return truncate("已完成「" + label + "」：" + summary.trim(), MAX_TOOL_SUMMARY_CHARS);
                }
            } catch (Exception ignored) {
                // fall through
            }
        }
        // 非 JSON：截断纯文本
        String plain = data.replaceAll("\\s+", " ").trim();
        if (plain.length() > 120) {
            plain = plain.substring(0, 120) + "…";
        }
        return truncate("已完成「" + label + "」：" + plain, MAX_TOOL_SUMMARY_CHARS);
    }

    /**
     * 将最终回答转为用户可读 Markdown；若是 JSON 则提取字段，并去掉过程汇报段落。
     *
     * @param raw 模型最终输出
     * @return 用户可见正文
     */
    public static String toAnswerDisplay(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        String trimmed = raw.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            return stripAgentMetaNarration(trimmed);
        }
        try {
            JSONObject obj = JSONUtil.parseObj(trimmed);
            StringBuilder sb = new StringBuilder();
            String summary = obj.getStr("summary");
            if (StringUtils.isNotBlank(summary)) {
                sb.append(summary.trim());
            }
            Object steps = obj.get("steps");
            if (steps instanceof JSONArray arr && !arr.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append("\n\n");
                }
                for (int i = 0; i < arr.size(); i++) {
                    String line = stringifyStep(arr.get(i));
                    if (StringUtils.isNotBlank(line)) {
                        sb.append(i + 1).append(". ").append(line).append('\n');
                    }
                }
            }
            JSONObject metadata = obj.getJSONObject("metadata");
            if (metadata != null) {
                String tip = metadata.getStr("next_action_suggestion");
                if (StringUtils.isNotBlank(tip)) {
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                    sb.append(tip.trim());
                }
            }
            String display = sb.isEmpty() ? trimmed : sb.toString().trim();
            return stripAgentMetaNarration(display);
        } catch (Exception e) {
            return stripAgentMetaNarration(trimmed);
        }
    }

    /**
     * 去掉开头连续的工具过程/第三人称汇报段落，保留真正答案正文。
     *
     * @param text 待清洗文本
     * @return 清洗后文本
     */
    public static String stripAgentMetaNarration(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        String[] parts = text.split("\\n\\s*\\n");
        int start = 0;
        while (start < parts.length && isMetaNarrationParagraph(parts[start])) {
            start++;
        }
        if (start == 0) {
            return text.trim();
        }
        if (start >= parts.length) {
            // 全文都像元叙述时，尽量保留最后一段（可能已是正文）
            return parts[parts.length - 1].trim();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < parts.length; i++) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(parts[i].trim());
        }
        return sb.toString().trim();
    }

    private static boolean isMetaNarrationParagraph(String paragraph) {
        if (StringUtils.isBlank(paragraph)) {
            return true;
        }
        String p = paragraph.trim();
        String[] markers = {
                "已成功获取",
                "已获取网页",
                "完全满足用户需求",
                "满足用户需求",
                "呈现给用户",
                "无需再调用",
                "不需要再调用",
                "现在即可输出最终回答",
                "输出最终回答",
                "根据工具返回",
                "根据工具结果",
                "工具执行结果"
        };
        for (String marker : markers) {
            if (p.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private static String stringifyStep(Object item) {
        if (item == null) {
            return "";
        }
        if (item instanceof JSONObject stepObj) {
            String name = stepObj.getStr("name", stepObj.getStr("title", ""));
            String detail = firstNonBlank(
                    stepObj.getStr("detail"),
                    stepObj.getStr("description"),
                    stepObj.getStr("action"));
            if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(detail)) {
                return name + "：" + detail;
            }
            return firstNonBlank(detail, name);
        }
        return String.valueOf(item).trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (StringUtils.isNotBlank(v)) {
                return v;
            }
        }
        return "";
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, Math.max(0, max - 1)) + "…";
    }

    /**
     * 判断文本是否像未清洗的工具原始 dump。
     *
     * @param text 待检测文本
     * @return true 表示疑似原始工具输出
     */
    public static boolean looksLikeRawToolDump(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return text.contains("工具 ") && text.contains("完成了它的任务")
                || lower.contains("\\\"title\\\"")
                || (text.contains("\"link\"") && text.contains("\"position\""));
    }
}
