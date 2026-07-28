package com.zhizhi.zhizhiaiagent.agent.model;

import com.zhizhi.zhizhiaiagent.agent.model.enums.AgentStatus;
import com.zhizhi.zhizhiaiagent.agent.model.exception.BusinessException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 基类，用于代理状态和执行
 */
@Slf4j
@Data
public abstract class BaseAgent {
    /**
     * 模型名称
     */
    private String name;

    /**
     * 模型状态
     */
    private AgentStatus status = AgentStatus.IDLE;

    /**
     * 模型系统提示
     */
    private String systemPrompt;

    /**
     * 模型下一步提示词
     */
    private String nextStepPrompt;

    /**
     * 执行控制，最大次数、当前次数
     */
    private int maxSteps = 10;
    private int currentStep = 1;

    /**
     * 模型执行器
     */
    private ChatClient chatClient;

    /**
     * 模型记忆(需要自主维护会话上下文）
     */
    private List<Message> messageList = new ArrayList<>();

    /**
     *运行代理
     */
    public String run(String userPrompt) {
        //状态处于非运行下
        if (this.status != AgentStatus.IDLE){
            throw new BusinessException("模型正在运行中，请勿重复运行", "MODEL_RUNNING");
        }
        //提示词非空
        if (StringUtils.isBlank(userPrompt)){
            throw new BusinessException("用户提示词不能为空", "USER_PROMPT_EMPTY");
        }
        //更改模型状态
        this.status = AgentStatus.RUNNING;
        //记录消息上下文
        this.messageList.add(new UserMessage(userPrompt));
        //保存返回结果
        List<String> results = new ArrayList<>();

        try {
            this.currentStep = 1;
            while (this.currentStep <= this.maxSteps && this.status != AgentStatus.FINISHED) {
                log.info("当前步骤：{}/{}", this.currentStep,this.maxSteps);
                // 执行单个步骤
                String stepResult = step();
                String result = "step " + this.currentStep + ": " + stepResult;
                results.add(result);

                this.currentStep++;
            }
            //检查是否超过最大限制步骤
            if (currentStep > maxSteps){
                this.status = AgentStatus.FINISHED;
                results.add("模型执行完成，已超过最大步骤限制");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            this.status = AgentStatus.ERROR;
            log.error("模型运行异常", e);
            throw new BusinessException("模型运行异常", "MODEL_RUN_ERROR");
        }finally {
            //清理资源
            this.cleanup();
        }
    }


    public SseEmitter runStream(String userPrompt) {
        SseEmitter sseEmitter = new SseEmitter(300000L); // 设置超时时间为 5 分钟
        CompletableFuture.runAsync(() -> {
            //状态处于非运行下
            try {
                if (this.status != AgentStatus.IDLE){
                    sseEmitter.send(AgentStreamEvent.error("模型正在运行中，请勿重复运行"));
                    sseEmitter.complete();
                    return;
                }
                //提示词非空
                if (StringUtils.isBlank(userPrompt)){
                    sseEmitter.send(AgentStreamEvent.error("用户提示词不能为空"));
                    sseEmitter.complete();
                    return;
                }
                //更改模型状态
                this.status = AgentStatus.RUNNING;
                //记录消息上下文
                this.messageList.add(new UserMessage(userPrompt));

                try {
                    // 1) 先通知前端进入「思考中...」
                    sseEmitter.send(AgentStreamEvent.thinkingStart());

                    String finalAnswer = null;
                    this.currentStep = 1;

                    while (this.currentStep <= this.maxSteps && this.status != AgentStatus.FINISHED) {
                        log.info("当前步骤：{}/{}", this.currentStep, this.maxSteps);

                        // 2) 步骤开始：实时推送进度
                        sseEmitter.send(AgentStreamEvent.thinkingProgress(
                                this.currentStep, this.maxSteps, "正在分析问题并规划行动…"));

                        if (this instanceof ReActAgent reActAgent) {
                            reActAgent.setLastStepFinalAnswer(false);
                            Boolean needAct = reActAgent.think();
                            String thinkText = reActAgent.getLastThinkText();
                            boolean isFinal = Boolean.FALSE.equals(needAct);
                            reActAgent.setLastStepFinalAnswer(isFinal);

                            if (StringUtils.isNotBlank(thinkText) || isFinal) {
                                String display = StringUtils.defaultString(thinkText).trim();
                                if (StringUtils.isBlank(display) && isFinal) {
                                    display = "已完成问题分析，正在组织最终回答…";
                                }
                                String chunk = "【步骤 " + this.currentStep + " · 思考】\n" + display;
                                sseEmitter.send(AgentStreamEvent.thinkingDelta(this.currentStep, chunk));
                            }

                            if (isFinal) {
                                finalAnswer = StringUtils.trimToNull(reActAgent.getFinalAnswer());
                                this.status = AgentStatus.FINISHED;
                                break;
                            }

                            // 3) 工具执行阶段：完成后立即推送
                            sseEmitter.send(AgentStreamEvent.thinkingProgress(
                                    this.currentStep, this.maxSteps, "正在调用工具执行…"));

                            String actResult = reActAgent.act();
                            if (StringUtils.isNotBlank(actResult)) {
                                String chunk = "【步骤 " + this.currentStep + " · 行动】\n" + actResult.trim();
                                sseEmitter.send(AgentStreamEvent.thinkingDelta(this.currentStep, chunk));
                            }

                            if (this.status == AgentStatus.FINISHED) {
                                finalAnswer = StringUtils.isNotBlank(actResult)
                                        ? actResult.trim()
                                        : "任务已完成。";
                                break;
                            }
                        } else {
                            String stepResult = step();
                            if (StringUtils.isNotBlank(stepResult)) {
                                String chunk = "【步骤 " + this.currentStep + "】\n" + stepResult.trim();
                                sseEmitter.send(AgentStreamEvent.thinkingDelta(this.currentStep, chunk));
                            }
                        }

                        this.currentStep++;
                    }

                    if (currentStep > maxSteps && this.status != AgentStatus.FINISHED) {
                        this.status = AgentStatus.FINISHED;
                        if (finalAnswer == null) {
                            finalAnswer = "已达到最大推理步骤，以下为当前结论。";
                        }
                    }

                    // 4) 思考完成（不重复推送全文）
                    sseEmitter.send(AgentStreamEvent.thinkingDone(null));

                    if (StringUtils.isBlank(finalAnswer) && this instanceof ReActAgent reActAgent) {
                        finalAnswer = StringUtils.trimToNull(reActAgent.getFinalAnswer());
                    }
                    if (StringUtils.isBlank(finalAnswer)) {
                        finalAnswer = "抱歉，本次未能生成有效回答。";
                    }
                    // 5) 最终回答
                    sseEmitter.send(AgentStreamEvent.answerDone(finalAnswer));

                    sseEmitter.complete();
                } catch (Exception e) {
                    this.status = AgentStatus.ERROR;
                    log.error("模型运行异常", e);
                    sseEmitter.send(AgentStreamEvent.error("模型运行异常：" + e.getMessage()));
                    sseEmitter.complete();
                }finally {
                    //清理资源
                    this.cleanup();
                }
            }catch (Exception e) {
                sseEmitter.completeWithError( e);
            }
        });
        sseEmitter.onTimeout(() -> {
            status = AgentStatus.ERROR;
            this.cleanup();
            log.warn("SSE连接超时");
        });
        sseEmitter.onCompletion(() -> {
            if (status == AgentStatus.RUNNING) {
                status = AgentStatus.FINISHED;
            }
            this.cleanup();
            log.info("SSE连接完成");
        });
        return sseEmitter;
    }

    /**
     * 执行单个步骤
     * @return
     */
    public abstract String step();


    /**
     * 清理资源
     */
    protected void cleanup() {
        //子类重写
    }
}
