package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息实体，对应 message 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message")
public class MessageEntity extends BaseEntity {

    /** 主键 ID */
    @TableId(type = IdType.INPUT)
    private String id;

    /** 所属会话 ID */
    private String conversationId;

    /** user / assistant / system / tool */
    private String role;

    /** 消息正文内容 */
    private String content;

    /** 扩展信息：思考链、工具调用摘要等（JSON 字符串） */
    private String metadata;
}
