package com.zhizhi.zhizhiaiagent.agent.multi;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 无工具 Planner：把用户任务拆成有序步骤 JSON。
 */
@Slf4j
@Component
public class PlannerAgent {

    public static final int MAX_STEPS = 5;

    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*}");

    private static final String SYSTEM = """
            你是任务规划 Agent（Planner）。只负责把用户需求拆成可执行步骤，不要调用工具，不要执行。
            严格输出一个 JSON 对象，不要 Markdown 代码围栏，不要其它说明文字：
            {"goal":"一句话目标","steps":["步骤1","步骤2"]}
            要求：
            1. steps 为中文短句，每步只做一件事；
            2. 最多 5 步；能 1～2 步完成就不要硬凑；
            3. 若涉及写文件/终端，单独成步并写清文件名或命令意图；
            4. 不要编造用户未要求的 PDF/搜索等额外产物。
            """;

    /**
     * 调用模型规划；解析失败时回退为单步「直接完成用户请求」。
     */
    public PlannerPlan plan(ChatModel chatModel, String userMessage) {
        ChatClient client = ChatClient.builder(chatModel).build();
        String raw = client.prompt()
                .system(SYSTEM)
                .user(userMessage == null ? "" : userMessage)
                .call()
                .content();
        return parsePlan(raw, userMessage);
    }

    /**
     * 解析 Planner 原始输出；供单测覆盖非法 JSON / 截断 steps。
     */
    public static PlannerPlan parsePlan(String raw, String fallbackUserMessage) {
        String json = extractJsonObject(raw);
        if (StringUtils.isNotBlank(json)) {
            try {
                JSONObject obj = JSONUtil.parseObj(json);
                String goal = StringUtils.trimToEmpty(obj.getStr("goal"));
                List<String> steps = new ArrayList<>();
                JSONArray arr = obj.getJSONArray("steps");
                if (arr != null) {
                    for (int i = 0; i < arr.size() && steps.size() < MAX_STEPS; i++) {
                        String s = StringUtils.trimToEmpty(arr.getStr(i));
                        if (StringUtils.isNotBlank(s)) {
                            steps.add(s);
                        }
                    }
                }
                if (StringUtils.isBlank(goal)) {
                    goal = StringUtils.defaultIfBlank(fallbackUserMessage, "完成用户任务");
                }
                if (!steps.isEmpty()) {
                    return new PlannerPlan(goal, List.copyOf(steps));
                }
            } catch (Exception e) {
                log.warn("parse planner json failed: {}", e.getMessage());
            }
        }
        String goal = StringUtils.defaultIfBlank(fallbackUserMessage, "完成用户任务");
        return new PlannerPlan(goal, List.of("直接完成用户请求：" + goal));
    }

    static String extractJsonObject(String raw) {
        if (!StringUtils.isNotBlank(raw)) {
            return "";
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "").trim();
        }
        if (text.startsWith("{") && text.endsWith("}")) {
            return text;
        }
        Matcher m = JSON_OBJECT.matcher(text);
        if (m.find()) {
            return m.group();
        }
        return "";
    }
}
