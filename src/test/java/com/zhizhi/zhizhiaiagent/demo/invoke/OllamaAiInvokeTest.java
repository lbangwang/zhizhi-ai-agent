package com.zhizhi.zhizhiaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.SpringBootTest;



import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OllamaAiInvokeTest {
    @Resource
    private ChatModel ollamaChatModel;


    @Test
    void qw() {
        ChatResponse call = ollamaChatModel.call(new Prompt("你好，你是谁啊！！，先帮忙介绍你是谁，让后在帮忙推送中国比较火的10个热门旅游地方," +
                "需要输出具体的地方和当地玩耍点和美食"));
        System.out.println(call.getResult());
        assertNotNull(call.getResult());
    }

}