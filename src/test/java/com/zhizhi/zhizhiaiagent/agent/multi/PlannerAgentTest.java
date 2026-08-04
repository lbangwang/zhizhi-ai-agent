package com.zhizhi.zhizhiaiagent.agent.multi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlannerAgentTest {

    @Test
    void parsePlan_validJson() {
        PlannerPlan plan = PlannerAgent.parsePlan(
                "{\"goal\":\"写文件\",\"steps\":[\"创建 hello.txt\",\"确认内容\"]}",
                "fallback");
        assertEquals("写文件", plan.goal());
        assertEquals(2, plan.steps().size());
        assertEquals("创建 hello.txt", plan.steps().get(0));
    }

    @Test
    void parsePlan_truncatesToMaxSteps() {
        StringBuilder sb = new StringBuilder("{\"goal\":\"g\",\"steps\":[");
        for (int i = 1; i <= 8; i++) {
            if (i > 1) sb.append(',');
            sb.append("\"s").append(i).append('"');
        }
        sb.append("]}");
        PlannerPlan plan = PlannerAgent.parsePlan(sb.toString(), "x");
        assertEquals(PlannerAgent.MAX_STEPS, plan.steps().size());
    }

    @Test
    void parsePlan_markdownFence() {
        PlannerPlan plan = PlannerAgent.parsePlan(
                "```json\n{\"goal\":\"目标\",\"steps\":[\"一步\"]}\n```",
                "x");
        assertEquals("目标", plan.goal());
        assertEquals(1, plan.steps().size());
    }

    @Test
    void parsePlan_invalidFallsBack() {
        PlannerPlan plan = PlannerAgent.parsePlan("不是 JSON", "用户原话");
        assertEquals("用户原话", plan.goal());
        assertEquals(1, plan.steps().size());
        assertTrue(plan.steps().get(0).contains("用户原话"));
    }
}
