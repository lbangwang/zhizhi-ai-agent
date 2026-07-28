package com.zhizhi.zhizhiaiagent.controller;

import com.zhizhi.zhizhiaiagent.agent.model.ZhizhiManus;
import com.zhizhi.zhizhiaiagent.agent.model.enums.AgentType;
import com.zhizhi.zhizhiaiagent.app.LoveApp;
import com.zhizhi.zhizhiaiagent.config.ChatModelRouter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@Slf4j
@RequestMapping("/zhizhi-ai")
public class AIController {


    @Resource
    private LoveApp loveApp;

    @Autowired
    private ToolCallback[] toolCallbacks;

    @Resource
    private ChatModelRouter chatModelRouter;

    @GetMapping("/doChatBySyn")
    public String doChatBySyn(String message, String chatId,
                              @RequestParam(defaultValue = "qwen") String model) {
        return loveApp.doChat(message, chatId);
    }


    @GetMapping(value = "/doChatBySynSSE", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatBySynSSE(String message, String chatId,
                                       @RequestParam(defaultValue = "qwen") String model) {
        return loveApp.doChatByStream(message, chatId, model);
    }


    @GetMapping(value = "/doChatByServerSSE")
    public Flux<ServerSentEvent<Object>> doChatByServerSSE(String message, String chatId,
                                                           @RequestParam(defaultValue = "qwen") String model) {
        Flux<String> stringFlux = loveApp.doChatByStream(message, chatId, model);
        return stringFlux.map(data -> ServerSentEvent.builder().data(data).build());
    }

    @GetMapping(value = "/doChatBySseEmitter")
    public SseEmitter doChatBySseEmitter(String message, String chatId,
                                         @RequestParam(defaultValue = "qwen") String model) {
        SseEmitter sseEmitter = new SseEmitter(300000L); // 设置超时时间为 5 分钟
        loveApp.doChatByStream(message, chatId, model).subscribe(data -> {
            try {
                sseEmitter.send(data);
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
            }
        }, sseEmitter::completeWithError, sseEmitter::complete);
        return sseEmitter;
    }

    @GetMapping(value = "/doChatByZhizhiManus")
    public SseEmitter doChatByZhizhiManus(String message,
                                          @RequestParam(defaultValue = "qwen") String model) {
        try {
            ChatModel chatModel = chatModelRouter.resolve(model);
            ZhizhiManus zhizhiManus = new ZhizhiManus(toolCallbacks, chatModel,
                    chatModelRouter.resolveChatOptions(model));
            log.info("ZhizhiManus using model={}", model);
            return zhizhiManus.runStream(message);
        } catch (Exception e) {
            log.error("ZhizhiManus start failed, model={}", model, e);
            SseEmitter sseEmitter = new SseEmitter(300000L);
            try {
                sseEmitter.send(e.getMessage());
                sseEmitter.complete();
            } catch (IOException ioException) {
                sseEmitter.completeWithError(ioException);
            }
            return sseEmitter;
        }
    }

    @GetMapping(value = "/stopChatByZhizhiManus")
    public SseEmitter stopChatByZhizhiManus(String message, String chatId, String type) {
        //AI 大模型一旦开始生成，服务端无法真正中断模型的计算，调用仍然会产生费用。
        // 我们能做的，是在 Flux 数据流层面中断内容的输出——即后端虽然还会收到模型返回的完整内容，但我们可以选择不再将后续数据推送给客户端。
        SseEmitter sseEmitter = new SseEmitter(300000L); // 设置超时时间为 5 分钟
        if (AgentType.PROFESSIONAL.getDesc().equals(type)) {
            loveApp.stopChat(chatId);

        }
        try {
            sseEmitter.send("停止输出");
            sseEmitter.complete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sseEmitter;
    }
}
