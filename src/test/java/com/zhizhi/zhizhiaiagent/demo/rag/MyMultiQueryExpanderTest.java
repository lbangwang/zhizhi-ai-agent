package com.zhizhi.zhizhiaiagent.demo.rag;

import com.zhizhi.zhizhiaiagent.advisor.MyLogAdvisor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class MyMultiQueryExpanderTest {

    @Autowired
    private MyMultiQueryExpander myMultiQueryExpander;

    @Autowired
    private ChatModel dashscopeChatModel;

    @Test
    void multiQueryExpander() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(dashscopeChatModel)
                .defaultSystem("您好 我是恋爱专家小李")
                //添加自定义advisor日志打印
                .defaultAdvisors(new MyLogAdvisor());
        myMultiQueryExpander.multiQueryExpander(chatClientBuilder);
    }
}