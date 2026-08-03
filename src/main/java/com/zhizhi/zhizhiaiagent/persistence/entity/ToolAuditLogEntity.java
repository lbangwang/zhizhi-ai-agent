package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_audit_log")
public class ToolAuditLogEntity extends BaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String userId;

    private String chatId;

    private String toolName;

    private String argumentsSummary;

    private String resultSummary;

    /** 1=成功 0=失败 */
    private Integer success;

    private Long durationMs;
}
