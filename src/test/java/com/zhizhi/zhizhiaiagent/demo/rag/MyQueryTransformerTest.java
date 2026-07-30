package com.zhizhi.zhizhiaiagent.demo.rag;

import com.zhizhi.zhizhiaiagent.advisor.MyLogAdvisor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@SpringBootTest
class MyQueryTransformerTest {

    @Autowired
    private MyQueryTransformer myQueryTransformer;

    @Autowired
    private ChatModel dashscopeChatModel;

    @Test
    void rewriteQueryTransformer() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(dashscopeChatModel)
                .defaultSystem("您好 我是面试官小助手小C李")
                //添加自定义advisor日志打印
                .defaultAdvisors(new MyLogAdvisor());
        Query query = Query.builder().text("我想学习AI相关技术如何开始呢？吴哈哈哈哈等等").build();
        myQueryTransformer.rewriteQueryTransformer(query,chatClientBuilder);
    }

    @Test
    void translationQueryTransformer() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(dashscopeChatModel)
                .defaultSystem("您好 我是面试官小助手小C李")
                //添加自定义advisor日志打印
                .defaultAdvisors(new MyLogAdvisor());
        Query query = Query.builder().text("我想学习AI相关技术如何开始呢").build();
        myQueryTransformer.translationQueryTransformer(query,chatClientBuilder);
    }

    @Test
    void compressionQueryTransformer() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(dashscopeChatModel)
                .defaultSystem("您好 我是面试官小助手小C李")
                //添加自定义advisor日志打印
                .defaultAdvisors(new MyLogAdvisor());
        Query query = Query.builder().text("我想学习AI相关技术如何开始呢？wwhhh啊啊啊啊").
                history(new UserMessage("学习MCP怎么开始？"),
                        new AssistantMessage("学习functionCall怎么开始？")).build();
        myQueryTransformer.compressionQueryTransformer(query,chatClientBuilder);
    }
}