package com.zhizhi.zhizhiaiagent.demo.invoke;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 本地 Ollama 连通性探测。默认关闭，避免云服务器无 Ollama 时拖垮启动。
 * 需要时在配置中设置：app.ollama.startup-probe=true
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ollama.startup-probe", havingValue = "true")
public class OllamaAiInvoke implements CommandLineRunner {

    @Resource
    private ChatModel ollamaChatModel;

    @Override
    public void run(String... args) {
        try {
            AssistantMessage output = ollamaChatModel.call(new Prompt("你好，我是ollama本地部署的千问"))
                    .getResult()
                    .getOutput();
            log.info("Ollama probe ok: {}", output.getText());
        } catch (Exception e) {
            log.warn("Ollama probe skipped/failed: {}", e.getMessage());
        }
    }
}
