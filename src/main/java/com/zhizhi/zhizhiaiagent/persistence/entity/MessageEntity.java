package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class MessageEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    /** user / assistant / system / tool */
    private String role;

    private String content;

    /** 扩展信息：思考链、工具调用摘要等（JSON 字符串） */
    private String metadata;

    private LocalDateTime createdAt;
}
