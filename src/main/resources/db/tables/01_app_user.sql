CREATE TABLE IF NOT EXISTS app_user (
    id              CHAR(32)     NOT NULL PRIMARY KEY COMMENT 'pk 32-char',
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(255) NULL,
    nickname        VARCHAR(64)  NULL,
    status          INT          NOT NULL DEFAULT 1 COMMENT '1=active 0=disabled',
    create_date     DATETIME     NOT NULL COMMENT 'create time',
    create_by       VARCHAR(64)  NULL COMMENT 'create by',
    update_date     DATETIME     NOT NULL COMMENT 'update time',
    update_by       VARCHAR(64)  NULL COMMENT 'update by',
    is_del          TINYINT      NOT NULL DEFAULT 0 COMMENT '0=normal 1=deleted',
    enterprise_id   CHAR(32)     NULL COMMENT 'tenant id 32-char, no default',
    UNIQUE KEY uk_app_user_username (username),
    KEY idx_app_user_enterprise_id (enterprise_id),
    KEY idx_app_user_is_del (is_del)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user';
