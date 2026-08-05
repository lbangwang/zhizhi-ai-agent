package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会话实体，对应 conversation 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("conversation")
public class ConversationEntity extends BaseEntity {

    /** 主键 ID */
    @TableId(type = IdType.INPUT)
    private String id;

    /** 业务会话 ID，32 位字符串，与前端 chatId 对齐 */
    private String chatId;

    /** 所属用户 ID */
    private String userId;

    /** LOVE_MASTER / SUPER_AGENT 等 */
    private String agentType;

    /** 会话标题 */
    private String title;

    /** 使用的模型名称 */
    private String model;

    /** 1=进行中 0=归档 */
    private Integer status;
}
