package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message")
public class MessageEntity extends BaseEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String conversationId;

    /** user / assistant / system / tool */
    private String role;

    private String content;

    /** 扩展信息：思考链、工具调用摘要等（JSON 字符串） */
    private String metadata;
}
