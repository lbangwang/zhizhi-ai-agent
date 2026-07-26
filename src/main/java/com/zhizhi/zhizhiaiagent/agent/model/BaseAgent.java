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
                    sseEmitter.send("模型正在运行中，请勿重复运行");
                    sseEmitter.complete();
                    return;
                }
                //提示词非空
                if (StringUtils.isBlank(userPrompt)){
                    sseEmitter.send("用户提示词不能为空");
                    sseEmitter.complete();
                    return;
                }
                //更改模型状态
                this.status = AgentStatus.RUNNING;
                //记录消息上下文
                this.messageList.add(new UserMessage(userPrompt));

                try {
                    this.currentStep = 1;
                    while (this.currentStep <= this.maxSteps && this.status != AgentStatus.FINISHED) {
                        log.info("当前步骤：{}/{}", this.currentStep,this.maxSteps);
                        // 执行单个步骤
                        String stepResult = step();
                        String result = "step " + this.currentStep + ": " + stepResult;
                        sseEmitter.send(result);

                        this.currentStep++;
                    }
                    //检查是否超过最大限制步骤
                    if (currentStep > maxSteps){
                        this.status = AgentStatus.FINISHED;
                        sseEmitter.send("模型执行完成，已超过最大步骤限制");
                    }
                    //正常完成
                    sseEmitter.complete();
                } catch (Exception e) {
                    this.status = AgentStatus.ERROR;
                    log.error("模型运行异常", e);
                    sseEmitter.send("模型运行异常：" + e.getMessage());
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
