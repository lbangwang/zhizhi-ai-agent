package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 运行 Trace 实体，对应 agent_trace 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_trace")
public class AgentTraceEntity extends BaseEntity {

    /** 主键 ID */
    @TableId(type = IdType.INPUT)
    private String id;

    /** Trace 唯一标识 */
    private String traceId;

    /** 所属用户 ID */
    private String userId;

    /** 关联会话 chatId */
    private String chatId;

    /** Agent 类型 */
    private String agentType;

    /** RUNNING / SUCCESS / CANCELLED / ERROR */
    private String status;

    /** 提示词 Token 数 */
    private Integer promptTokens;

    /** 补全 Token 数 */
    private Integer completionTokens;

    /** 总 Token 数 */
    private Integer totalTokens;

    /** 运行耗时（毫秒） */
    private Long durationMs;

    /** Agent 执行步数 */
    private Integer stepCount;

    /** 失败时的错误信息 */
    private String errorMessage;
}
