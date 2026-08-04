package com.zhizhi.zhizhiaiagent.agent.stop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatStopSignalServiceTest {

    @Test
    void localMode_requestStopAndClear() {
        ChatStopSignalService service = new ChatStopSignalService(null, false, "zhizhi:chat:stop:", 600);
        String chatId = "test-chat-1";
        assertFalse(service.shouldStop(chatId));
        service.requestStop(chatId);
        assertTrue(service.shouldStop(chatId));
        service.clear(chatId);
        assertFalse(service.shouldStop(chatId));
    }

    @Test
    void emptyChatId_ignored() {
        ChatStopSignalService service = new ChatStopSignalService(null, false, "zhizhi:chat:stop:", 600);
        service.requestStop("  ");
        assertFalse(service.shouldStop(" "));
        assertFalse(service.shouldStop(null));
    }
}
