package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_trace")
public class AgentTraceEntity extends BaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String traceId;

    private String userId;

    private String chatId;

    private String agentType;

    /** RUNNING / SUCCESS / CANCELLED / ERROR */
    private String status;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Long durationMs;

    private Integer stepCount;

    private String errorMessage;
}
