package com.zhizhi.zhizhiaiagent.agent.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * ReActAgent模式，实现思考、行动两个核心步骤
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public abstract class ReActAgent extends BaseAgent{

    /**
     * 最近一步是否已产出最终回答（未再调用工具）
     */
    private boolean lastStepFinalAnswer = false;

    /**
     * 思考步骤：处理当前状态并执行下一步骤
     */
    public abstract Boolean think();

    /**
     * 行动步骤：执行思考后的动作
     */
    public abstract String act();

    /**
     * 获取最近一次思考的可展示文本（子类可覆盖）
     */
    public String getLastThinkText() {
        return "";
    }

    /**
     * 执行单个步骤：思考➕行动
     */
    @Override
    public String step() {
        try {
            this.lastStepFinalAnswer = false;
            Boolean think = this.think();
            if (!think){
                //如果没有工具调用，直接回复
                this.lastStepFinalAnswer = true;
                return getFinalAnswer();

//                return "思考完成，无需行动！！！";
            }
            return this.act();
        } catch (Exception e) {
            log.info("执行步骤异常：{}", e.getMessage());
            this.lastStepFinalAnswer = false;
            return "执行步骤异常：" + e.getMessage();
        }
    }

    public String getFinalAnswer() {
        List<Message> messageList = getMessageList();
        // 从后往前找最后一个 AssistantMessage
        for (int i = messageList.size() - 1; i >= 0; i--) {
            Message msg = messageList.get(i);
            if (msg instanceof AssistantMessage) {
                String text = msg.getText();
                if (StringUtils.isNotBlank(text)) {
                    return text;
                }
            }
        }
        return "抱歉，我没有生成有效回复。";
    }
}
