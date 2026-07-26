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
                .defaultSystem("您好 我是恋爱专家小李")
                //添加自定义advisor日志打印
                .defaultAdvisors(new MyLogAdvisor());
        Query query = Query.builder().text("我和女朋友吵架了，怎么办？wwhhh啊啊啊啊").build();
        myQueryTransformer.rewriteQueryTransformer(query,chatClientBuilder);
    }

    @Test
    void translationQueryTransformer() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(dashscopeChatModel)
                .defaultSystem("您好 我是恋爱专家小李")
                //添加自定义advisor日志打印
                .defaultAdvisors(new MyLogAdvisor());
        Query query = Query.builder().text("我和女朋友吵架了，怎么办").build();
        myQueryTransformer.translationQueryTransformer(query,chatClientBuilder);
    }

    @Test
    void compressionQueryTransformer() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(dashscopeChatModel)
                .defaultSystem("您好 我是恋爱专家小李")
                //添加自定义advisor日志打印
                .defaultAdvisors(new MyLogAdvisor());
        Query query = Query.builder().text("我和女朋友吵架了，怎么办？wwhhh啊啊啊啊").
                history(new UserMessage("关系不亲密怎么办"),
                        new AssistantMessage("恋爱问题怎么办")).build();
        myQueryTransformer.compressionQueryTransformer(query,chatClientBuilder);
    }
}