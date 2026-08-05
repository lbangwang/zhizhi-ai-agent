package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工具调用审计日志实体，对应 tool_audit_log 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_audit_log")
public class ToolAuditLogEntity extends BaseEntity {

    /** 主键 ID */
    @TableId(type = IdType.INPUT)
    private String id;

    /** 调用用户 ID */
    private String userId;

    /** 关联会话 chatId */
    private String chatId;

    /** 工具名称 */
    private String toolName;

    /** 入参摘要 */
    private String argumentsSummary;

    /** 返回结果摘要 */
    private String resultSummary;

    /** 1=成功 0=失败 */
    private Integer success;

    /** 调用耗时（毫秒） */
    private Long durationMs;
}
