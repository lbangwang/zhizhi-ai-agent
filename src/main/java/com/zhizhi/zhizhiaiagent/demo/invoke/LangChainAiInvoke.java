package com.zhizhi.zhizhiaiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;

public class LangChainAiInvoke {

    public static void main(String[] args) {
        QwenChatModel qwenModel = QwenChatModel.builder()
                .apiKey(ApiKeyTest.API_KEY_TEST)
                .modelName("qwen-max")
                .build();
        String answer = qwenModel.chat("你好，我是zz");
        System.out.println(answer);
    }
}

