CREATE TABLE IF NOT EXISTS message (
    id                 CHAR(32)     NOT NULL PRIMARY KEY COMMENT 'pk 32-char',
    conversation_id    CHAR(32)     NOT NULL COMMENT 'conversation pk',
    role               VARCHAR(32)  NOT NULL COMMENT 'user assistant system tool',
    content            LONGTEXT     NOT NULL,
    metadata           LONGTEXT     NULL COMMENT 'extra json',
    create_date        DATETIME     NOT NULL COMMENT 'create time',
    create_by          VARCHAR(64)  NULL COMMENT 'create by',
    update_date        DATETIME     NOT NULL COMMENT 'update time',
    update_by          VARCHAR(64)  NULL COMMENT 'update by',
    is_del             TINYINT      NOT NULL DEFAULT 0 COMMENT '0=normal 1=deleted',
    enterprise_id      CHAR(32)     NULL COMMENT 'tenant id 32-char, no default',
    KEY idx_message_conversation_id (conversation_id),
    KEY idx_message_enterprise_id (enterprise_id),
    KEY idx_message_is_del (is_del)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='message';
