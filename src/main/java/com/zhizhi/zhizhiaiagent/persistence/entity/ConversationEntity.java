package com.zhizhi.zhizhiaiagent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation")
public class ConversationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务会话 ID，与前端 chatId 对齐 */
    private String chatId;

    private Long userId;

    /** LOVE_MASTER / SUPER_AGENT 等 */
    private String agentType;

    private String title;

    private String model;

    /** 1=进行中 0=归档 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
