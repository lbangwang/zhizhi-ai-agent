package com.zhizhi.zhizhiaiagent.agent.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * ReAct 模式抽象：先思考再按需行动。
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public abstract class ReActAgent extends BaseAgent {

    /** 最近一步是否已产出最终回答（未再调用工具） */
    private boolean lastStepFinalAnswer = false;

    /**
     * 思考：分析当前状态，决定是否需要行动。
     *
     * @return true 需要调用 {@link #act()}；false 表示可直接作为最终回答
     */
    public abstract Boolean think();

    /**
     * 行动：执行思考阶段规划的动作（如工具调用）。
     *
     * @return 行动结果摘要
     */
    public abstract String act();

    /**
     * 获取最近一次思考的可展示文本；子类可覆盖。
     *
     * @return 思考文案，默认空串
     */
    public String getLastThinkText() {
        return "";
    }

    /**
     * 同步单步执行：先 think，无需行动则返回最终回答，否则执行 act。
     *
     * @return 本步结果文本
     */
    @Override
    public String step() {
        try {
            this.lastStepFinalAnswer = false;
            Boolean needAct = this.think();
            if (Boolean.FALSE.equals(needAct)) {
                this.lastStepFinalAnswer = true;
                return getFinalAnswer();
            }
            return this.act();
        } catch (Exception e) {
            log.info("执行步骤异常：{}", e.getMessage());
            this.lastStepFinalAnswer = false;
            return "执行步骤异常：" + e.getMessage();
        }
    }

    /**
     * 从会话消息中取最近一条非空助手回复作为最终回答。
     *
     * @return 最终回答文本
     */
    public String getFinalAnswer() {
        List<Message> messages = getMessageList();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg instanceof AssistantMessage assistantMessage) {
                String text = assistantMessage.getText();
                if (StringUtils.isNotBlank(text)) {
                    return text;
                }
            }
        }
        return "抱歉，我没有生成有效回复。";
    }
}
