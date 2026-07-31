-- D2: 会话持久化表结构（本地 MySQL 就绪后执行）
-- 建库示例：
--   CREATE DATABASE IF NOT EXISTS zhizhi_ai_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--
-- MyBatis 不会自动建表：启用 MYSQL_ENABLED=true 前请先执行本脚本。

CREATE TABLE IF NOT EXISTS app_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(255) NULL,
    nickname        VARCHAR(64)  NULL,
    status          INT          NOT NULL DEFAULT 1 COMMENT '1=正常 0=禁用',
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NOT NULL,
    UNIQUE KEY uk_app_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE IF NOT EXISTS conversation (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    chat_id         VARCHAR(64)  NOT NULL COMMENT '业务会话ID，对齐前端 chatId',
    user_id         BIGINT       NULL,
    agent_type      VARCHAR(32)  NOT NULL COMMENT 'LOVE_MASTER / SUPER_AGENT',
    title           VARCHAR(128) NOT NULL,
    model           VARCHAR(64)  NULL,
    status          INT          NOT NULL DEFAULT 1 COMMENT '1=进行中 0=归档',
    created_at      DATETIME     NOT NULL,
    updated_at      DATETIME     NOT NULL,
    UNIQUE KEY uk_conversation_chat_id (chat_id),
    KEY idx_conversation_user_id (user_id),
    KEY idx_conversation_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话';

CREATE TABLE IF NOT EXISTS message (
    id                 BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    conversation_id    BIGINT       NOT NULL,
    role               VARCHAR(32)  NOT NULL COMMENT 'user/assistant/system/tool',
    content            LONGTEXT     NOT NULL,
    metadata           LONGTEXT     NULL COMMENT '思考链/工具摘要等 JSON',
    created_at         DATETIME     NOT NULL,
    KEY idx_message_conversation_id (conversation_id),
    CONSTRAINT fk_message_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversation (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息';
