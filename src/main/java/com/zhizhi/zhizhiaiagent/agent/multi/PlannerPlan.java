package com.zhizhi.zhizhiaiagent.agent.multi;

import java.util.List;

/**
 * Planner 输出：目标 + 有序步骤（最多 {@link PlannerAgent#MAX_STEPS} 步）。
 */
public record PlannerPlan(String goal, List<String> steps) {
}
