package com.zhizhi.zhizhiaiagent.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhizhi.zhizhiaiagent.agent.model.ZhizhiManus;
import com.zhizhi.zhizhiaiagent.agent.model.enums.AgentType;
import com.zhizhi.zhizhiaiagent.agent.observability.AgentToolObservabilityService;
import com.zhizhi.zhizhiaiagent.agent.stop.ChatStopSignalService;
import com.zhizhi.zhizhiaiagent.app.LoveApp;
import com.zhizhi.zhizhiaiagent.config.ChatModelRouter;
import com.zhizhi.zhizhiaiagent.demo.rag.MyQueryTransformer;
import com.zhizhi.zhizhiaiagent.persistence.service.AgentTraceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Objects;

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

    @Autowired
    private MyQueryTransformer myQueryTransformer;

    @Autowired
    private ChatStopSignalService chatStopSignalService;

    @Autowired(required = false)
    private AgentToolObservabilityService toolObservabilityService;

    @Autowired(required = false)
    private AgentTraceService agentTraceService;

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
                                          String chatId,
                                          @RequestParam(defaultValue = "qwen") String model) {
        try {
            ChatModel chatModel = chatModelRouter.resolve(model);
            // 提示词增强：重写用户问题后再交给超级智能体
            String enhancedMessage = myQueryTransformer.rewriteUserMessage(message, chatModel);
            ZhizhiManus zhizhiManus = new ZhizhiManus(toolCallbacks, chatModel,
                    chatModelRouter.resolveChatOptions(model),chatId);
            //第一次打开对话框，清除停止信号对应key，并把停止service传入后续步骤
            if (StringUtils.hasText(chatId)) {
                chatStopSignalService.clear(chatId);
                zhizhiManus.setChatId(chatId.trim());
                zhizhiManus.setStopSignalService(chatStopSignalService);
            }
            // 传入：工具审计 / 产物入库需要参数userId、service
            try {
                if (StpUtil.isLogin()) {
                    zhizhiManus.setUserId(StpUtil.getLoginIdAsString());
                }
            } catch (Exception ignored) {
                // Sa-Token 未启用时忽略
            }
            if (!Objects.isNull(toolObservabilityService)) {
                zhizhiManus.setToolObservabilityService(toolObservabilityService);
            }
            // 添加：链路追踪需要参数智能体类型、service
            if (!Objects.isNull(agentTraceService)) {
                zhizhiManus.setAgentTraceService(agentTraceService);
                zhizhiManus.setAgentType("SUPER_AGENT");
            }
            log.info("ZhizhiManus using model={}, chatId={}", model, chatId);
            return zhizhiManus.runStream(enhancedMessage);
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
        // 模型单次 call 无法硬中断；D5 通过 Redis/内存停止信号阻止 Agent 继续下一步。
        SseEmitter sseEmitter = new SseEmitter(300000L);
        chatStopSignalService.requestStop(chatId);
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
