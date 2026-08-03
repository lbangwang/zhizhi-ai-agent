-- D2: 会话持久化表结构（本地 MySQL 就绪后执行）
-- 建库示例：
--   CREATE DATABASE IF NOT EXISTS zhizhi_ai_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--
-- 所有业务 ID（id / chat_id / user_id / conversation_id / enterprise_id）均为 32 位字符串，
-- 由应用或前端代码生成（UUID 去横线），不自增。
-- MyBatis 不会自动建表：启用 MYSQL_ENABLED=true 前请先执行本脚本。

CREATE TABLE IF NOT EXISTS app_user (
    id              CHAR(32)     NOT NULL PRIMARY KEY COMMENT '主键，32位字符串',
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(255) NULL,
    nickname        VARCHAR(64)  NULL,
    status          INT          NOT NULL DEFAULT 1 COMMENT '1=正常 0=禁用',
    create_date     DATETIME     NOT NULL COMMENT '创建时间',
    create_by       VARCHAR(64)  NULL COMMENT '创建人',
    update_date     DATETIME     NOT NULL COMMENT '更新时间',
    update_by       VARCHAR(64)  NULL COMMENT '更新人',
    is_del          TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未删除 1=已删除',
    enterprise_id   CHAR(32)     NULL COMMENT '企业/租户ID，32位字符串，无默认值',
    UNIQUE KEY uk_app_user_username (username),
    KEY idx_app_user_enterprise_id (enterprise_id),
    KEY idx_app_user_is_del (is_del)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE IF NOT EXISTS conversation (
    id              CHAR(32)     NOT NULL PRIMARY KEY COMMENT '主键，32位字符串',
    chat_id         CHAR(32)     NOT NULL COMMENT '业务会话ID，32位字符串，对齐前端 chatId',
    user_id         CHAR(32)     NULL COMMENT '用户ID，32位字符串',
    agent_type      VARCHAR(32)  NOT NULL COMMENT '智能体类型',
    title           VARCHAR(128) NOT NULL,
    model           VARCHAR(64)  NULL,
    status          INT          NOT NULL DEFAULT 1 COMMENT '1=进行中 0=归档',
    create_date     DATETIME     NOT NULL COMMENT '创建时间',
    create_by       VARCHAR(64)  NULL COMMENT '创建人',
    update_date     DATETIME     NOT NULL COMMENT '更新时间',
    update_by       VARCHAR(64)  NULL COMMENT '更新人',
    is_del          TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未删除 1=已删除',
    enterprise_id   CHAR(32)     NULL COMMENT '企业/租户ID，32位字符串，无默认值',
    UNIQUE KEY uk_conversation_chat_id (chat_id),
    KEY idx_conversation_user_id (user_id),
    KEY idx_conversation_update_date (update_date),
    KEY idx_conversation_enterprise_id (enterprise_id),
    KEY idx_conversation_is_del (is_del)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话';

CREATE TABLE IF NOT EXISTS message (
    id                 CHAR(32)     NOT NULL PRIMARY KEY COMMENT '主键，32位字符串',
    conversation_id    CHAR(32)     NOT NULL COMMENT '会话主键ID',
    role               VARCHAR(32)  NOT NULL COMMENT 'user/assistant/system/tool',
    content            LONGTEXT     NOT NULL,
    metadata           LONGTEXT     NULL COMMENT '思考链/工具摘要等 JSON',
    create_date        DATETIME     NOT NULL COMMENT '创建时间',
    create_by          VARCHAR(64)  NULL COMMENT '创建人',
    update_date        DATETIME     NOT NULL COMMENT '更新时间',
    update_by          VARCHAR(64)  NULL COMMENT '更新人',
    is_del             TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未删除 1=已删除',
    enterprise_id      CHAR(32)     NULL COMMENT '企业/租户ID，32位字符串，无默认值',
    KEY idx_message_conversation_id (conversation_id),
    KEY idx_message_enterprise_id (enterprise_id),
    KEY idx_message_is_del (is_del)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息';

-- W2: 知识库文档元数据（向量正文落在 SimpleVectorStore 文件中）
CREATE TABLE IF NOT EXISTS kb_document (
    id              CHAR(32)     NOT NULL PRIMARY KEY COMMENT '主键，32位字符串',
    user_id         CHAR(32)     NOT NULL COMMENT '所属用户ID',
    title           VARCHAR(256) NOT NULL COMMENT '展示标题',
    filename        VARCHAR(256) NOT NULL COMMENT '原始文件名',
    content_type    VARCHAR(128) NULL COMMENT 'MIME 类型',
    file_path       VARCHAR(512) NOT NULL COMMENT '本地存储相对路径',
    chunk_count     INT          NOT NULL DEFAULT 0 COMMENT '切片数量',
    chunk_ids       LONGTEXT     NULL COMMENT 'VectorStore 文档ID列表 JSON',
    status          INT          NOT NULL DEFAULT 1 COMMENT '1=就绪 0=失败',
    error_message   VARCHAR(512) NULL COMMENT '失败原因',
    create_date     DATETIME     NOT NULL COMMENT '创建时间',
    create_by       VARCHAR(64)  NULL COMMENT '创建人',
    update_date     DATETIME     NOT NULL COMMENT '更新时间',
    update_by       VARCHAR(64)  NULL COMMENT '更新人',
    is_del          TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未删除 1=已删除',
    enterprise_id   CHAR(32)     NULL COMMENT '企业/租户ID',
    KEY idx_kb_document_user_id (user_id),
    KEY idx_kb_document_update_date (update_date),
    KEY idx_kb_document_is_del (is_del)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档元数据';

-- W2: Agent 产物元数据
CREATE TABLE IF NOT EXISTS artifact (
    id              CHAR(32)     NOT NULL PRIMARY KEY COMMENT '主键，32位字符串',
    user_id         CHAR(32)     NOT NULL COMMENT '所属用户ID',
    chat_id         CHAR(32)     NULL COMMENT '关联会话 chatId',
    tool_name       VARCHAR(64)  NULL COMMENT '产生该产物的工具名',
    file_name       VARCHAR(256) NOT NULL COMMENT '文件名',
    content_type    VARCHAR(128) NULL COMMENT 'MIME 类型',
    file_path       VARCHAR(512) NOT NULL COMMENT '受管存储绝对/相对路径',
    file_size       BIGINT       NULL COMMENT '字节大小',
    source_path     VARCHAR(512) NULL COMMENT '工具原始输出路径',
    create_date     DATETIME     NOT NULL COMMENT '创建时间',
    create_by       VARCHAR(64)  NULL COMMENT '创建人',
    update_date     DATETIME     NOT NULL COMMENT '更新时间',
    update_by       VARCHAR(64)  NULL COMMENT '更新人',
    is_del          TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未删除 1=已删除',
    enterprise_id   CHAR(32)     NULL COMMENT '企业/租户ID',
    KEY idx_artifact_user_id (user_id),
    KEY idx_artifact_chat_id (chat_id),
    KEY idx_artifact_update_date (update_date),
    KEY idx_artifact_is_del (is_del)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 产物元数据';

-- W2: 工具调用审计日志
CREATE TABLE IF NOT EXISTS tool_audit_log (
    id                  CHAR(32)     NOT NULL PRIMARY KEY COMMENT '主键，32位字符串',
    user_id             CHAR(32)     NULL COMMENT '调用用户ID',
    chat_id             CHAR(32)     NULL COMMENT '关联会话 chatId',
    tool_name           VARCHAR(64)  NOT NULL COMMENT '工具名',
    arguments_summary   VARCHAR(1000) NULL COMMENT '入参摘要（截断）',
    result_summary      VARCHAR(2000) NULL COMMENT '结果摘要（截断）',
    success             TINYINT      NOT NULL DEFAULT 1 COMMENT '1=成功 0=失败',
    duration_ms         BIGINT       NULL COMMENT '本批工具耗时毫秒',
    create_date         DATETIME     NOT NULL COMMENT '创建时间',
    create_by           VARCHAR(64)  NULL COMMENT '创建人',
    update_date         DATETIME     NOT NULL COMMENT '更新时间',
    update_by           VARCHAR(64)  NULL COMMENT '更新人',
    is_del              TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未删除 1=已删除',
    enterprise_id       CHAR(32)     NULL COMMENT '企业/租户ID',
    KEY idx_tool_audit_user_id (user_id),
    KEY idx_tool_audit_chat_id (chat_id),
    KEY idx_tool_audit_tool_name (tool_name),
    KEY idx_tool_audit_create_date (create_date),
    KEY idx_tool_audit_is_del (is_del)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具调用审计日志';
