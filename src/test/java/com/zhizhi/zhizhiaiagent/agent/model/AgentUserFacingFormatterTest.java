package com.zhizhi.zhizhiaiagent.agent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentUserFacingFormatterTest {

    @Test
    void toolDoneSummary_hitlRejected() {
        String raw = "Error: 危险工具「writeFile」未执行（用户拒绝）。";
        assertEquals("写入文件 已拒绝", AgentUserFacingFormatter.toolDoneSummary("writeFile", raw));
        assertTrue(AgentUserFacingFormatter.isHitlRejectedResult(raw));
    }

    @Test
    void toolDoneSummary_quotedJsonString() {
        String raw = "\"Error: 危险工具「writeFile」未执行（用户拒绝）。\"";
        assertEquals("写入文件 已拒绝", AgentUserFacingFormatter.toolDoneSummary("writeFile", raw));
    }

    @Test
    void toolDoneSummary_blankIsEndedNotCompleted() {
        assertEquals("写入文件 已结束", AgentUserFacingFormatter.toolDoneSummary("writeFile", null));
        assertEquals("写入文件 已结束", AgentUserFacingFormatter.toolDoneSummary("writeFile", "  "));
        assertFalse(AgentUserFacingFormatter.isHitlRejectedResult(null));
    }

    @Test
    void toolDoneSummary_success() {
        assertEquals(
                "写入文件 已完成",
                AgentUserFacingFormatter.toolDoneSummary("writeFile", "File written successfully to: /tmp/a.txt"));
    }

    @Test
    void toToolResultDisplay_rejectDoesNotSayCompleted() {
        String display = AgentUserFacingFormatter.toToolResultDisplay(
                "writeFile", "Error: 危险工具「writeFile」未执行（用户拒绝）。");
        assertTrue(display.contains("未执行") || display.contains("拒绝"));
        assertFalse(display.startsWith("已完成"));
    }
}
