package com.zhizhi.zhizhiaiagent.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhizhi.zhizhiaiagent.agent.model.ZhizhiManus;
import com.zhizhi.zhizhiaiagent.agent.model.enums.AgentType;
import com.zhizhi.zhizhiaiagent.agent.multi.MultiAgentOrchestrator;
import com.zhizhi.zhizhiaiagent.agent.observability.AgentToolObservabilityService;
import com.zhizhi.zhizhiaiagent.agent.stop.ChatStopSignalService;
import com.zhizhi.zhizhiaiagent.app.LoveApp;
import com.zhizhi.zhizhiaiagent.config.ChatModelRouter;
import com.zhizhi.zhizhiaiagent.demo.rag.MyQueryTransformer;
import com.zhizhi.zhizhiaiagent.persistence.service.AgentTraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "AI 对话")
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

    @Autowired
    private MultiAgentOrchestrator multiAgentOrchestrator;

    @Operation(summary = "同步对话", description = "同步返回完整 AI 回复文本。")
    @GetMapping("/doChatBySyn")
    public String doChatBySyn(
            @Parameter(description = "用户消息内容", required = true, example = "你好，请介绍一下自己")
            String message,
            @Parameter(description = "会话 ID，用于关联上下文", required = true,
                    example = "a1b2c3d4e5f6789012345678abcdef01")
            String chatId,
            @Parameter(description = "模型名称", required = false, example = "qwen")
            @RequestParam(defaultValue = "qwen") String model) {
        return loveApp.doChat(message, chatId);
    }


    @Operation(summary = "SSE 流式对话（Reactor Flux）", description = "以 Server-Sent Events 流式返回 AI 回复片段。")
    @GetMapping(value = "/doChatBySynSSE", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatBySynSSE(
            @Parameter(description = "用户消息内容", required = true, example = "你好，请介绍一下自己")
            String message,
            @Parameter(description = "会话 ID，用于关联上下文", required = true,
                    example = "a1b2c3d4e5f6789012345678abcdef01")
            String chatId,
            @Parameter(description = "模型名称", required = false, example = "qwen")
            @RequestParam(defaultValue = "qwen") String model) {
        return loveApp.doChatByStream(message, chatId, model);
    }


    @Operation(summary = "SSE 流式对话（ServerSentEvent 包装）",
            description = "以标准 ServerSentEvent 格式流式返回 AI 回复。")
    @GetMapping(value = "/doChatByServerSSE")
    public Flux<ServerSentEvent<Object>> doChatByServerSSE(
            @Parameter(description = "用户消息内容", required = true, example = "你好，请介绍一下自己")
            String message,
            @Parameter(description = "会话 ID，用于关联上下文", required = true,
                    example = "a1b2c3d4e5f6789012345678abcdef01")
            String chatId,
            @Parameter(description = "模型名称", required = false, example = "qwen")
            @RequestParam(defaultValue = "qwen") String model) {
        Flux<String> stringFlux = loveApp.doChatByStream(message, chatId, model);
        return stringFlux.map(data -> ServerSentEvent.builder().data(data).build());
    }

    @Operation(summary = "SSE 流式对话（SseEmitter）",
            description = "以 Spring SseEmitter 流式推送 AI 回复，超时 5 分钟。")
    @GetMapping(value = "/doChatBySseEmitter")
    public SseEmitter doChatBySseEmitter(
            @Parameter(description = "用户消息内容", required = true, example = "你好，请介绍一下自己")
            String message,
            @Parameter(description = "会话 ID，用于关联上下文", required = true,
                    example = "a1b2c3d4e5f6789012345678abcdef01")
            String chatId,
            @Parameter(description = "模型名称", required = false, example = "qwen")
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

    @Operation(summary = "超级智能体对话（ZhizhiManus SSE）",
            description = "通过 ZhizhiManus 超级智能体流式对话，支持工具调用、HITL 与 Trace 可观测。")
    @GetMapping(value = "/doChatByZhizhiManus")
    public SseEmitter doChatByZhizhiManus(
            @Parameter(description = "用户消息内容", required = true, example = "帮我分析这份数据")
            String message,
            @Parameter(description = "会话 ID，用于关联上下文与停止信号", required = true,
                    example = "a1b2c3d4e5f6789012345678abcdef01")
            String chatId,
            @Parameter(description = "模型名称", required = false, example = "qwen")
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

    /**
     * W4：Planner → Worker 多 Agent（SSE 事件格式与 Manus 一致，停止接口复用 stopChatByZhizhiManus）。
     */
    @Operation(summary = "多 Agent 对话（Planner → Worker SSE）",
            description = "通过 Planner → Worker 多 Agent 编排流式对话，SSE 格式与 Manus 一致。")
    @GetMapping(value = "/doChatByMultiAgent")
    public SseEmitter doChatByMultiAgent(
            @Parameter(description = "用户消息内容", required = true, example = "帮我制定一份项目计划")
            String message,
            @Parameter(description = "会话 ID，用于关联上下文与停止信号", required = true,
                    example = "a1b2c3d4e5f6789012345678abcdef01")
            String chatId,
            @Parameter(description = "模型名称", required = false, example = "qwen")
            @RequestParam(defaultValue = "qwen") String model) {
        try {
            ChatModel chatModel = chatModelRouter.resolve(model);
            String userId = null;
            try {
                if (StpUtil.isLogin()) {
                    userId = StpUtil.getLoginIdAsString();
                }
            } catch (Exception ignored) {
                // Sa-Token 未启用时忽略
            }
            log.info("MultiAgent using model={}, chatId={}", model, chatId);
            return multiAgentOrchestrator.runStream(
                    message,
                    chatId,
                    userId,
                    chatModel,
                    chatModelRouter.resolveChatOptions(model),
                    toolCallbacks,
                    toolObservabilityService,
                    agentTraceService);
        } catch (Exception e) {
            log.error("MultiAgent start failed, model={}", model, e);
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

    @Operation(summary = "停止 AI 输出",
            description = "请求停止指定会话的 AI 流式输出；专业版智能体需传入 type 参数。")
    @GetMapping(value = "/stopChatByZhizhiManus")
    public SseEmitter stopChatByZhizhiManus(
            @Parameter(description = "用户消息（停止时可留空）", required = false)
            String message,
            @Parameter(description = "要停止输出的会话 ID", required = true,
                    example = "a1b2c3d4e5f6789012345678abcdef01")
            String chatId,
            @Parameter(description = "智能体类型，专业版传对应 desc 以触发额外停止逻辑", required = false,
                    example = "专业版")
            String type) {
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
