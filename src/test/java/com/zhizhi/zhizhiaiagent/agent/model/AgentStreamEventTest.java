package com.zhizhi.zhizhiaiagent.agent.model;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentStreamEventTest {

    @Test
    void hitlRequired_hasRequiredFields() {
        String json = AgentStreamEvent.hitlRequired("id1", "writeFile", "{\"fileName\":\"a.txt\"}", 1);
        JSONObject obj = JSONUtil.parseObj(json);
        assertEquals("hitl_required", obj.getStr("type"));
        assertEquals("id1", obj.getStr("approvalId"));
        assertEquals("writeFile", obj.getStr("tool"));
        assertEquals("{\"fileName\":\"a.txt\"}", obj.getStr("arguments"));
        assertEquals(1, obj.getInt("step"));
    }

    @Test
    void toolDone_includesOutcome() {
        String json = AgentStreamEvent.toolDone(2, "writeFile", "写入文件 已拒绝", "rejected");
        JSONObject obj = JSONUtil.parseObj(json);
        assertEquals("tool_done", obj.getStr("type"));
        assertEquals("writeFile", obj.getStr("tool"));
        assertEquals("写入文件 已拒绝", obj.getStr("text"));
        assertEquals("rejected", obj.getStr("outcome"));
        assertEquals(2, obj.getInt("step"));
    }

    @Test
    void error_event() {
        String json = AgentStreamEvent.error("连接超时，请重试或缩短任务。");
        JSONObject obj = JSONUtil.parseObj(json);
        assertEquals("error", obj.getStr("type"));
        assertTrue(obj.getStr("text").contains("超时"));
    }
}
