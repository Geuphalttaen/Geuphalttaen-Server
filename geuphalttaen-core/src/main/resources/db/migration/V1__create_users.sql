CREATE TABLE users
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    provider    VARCHAR(20)  NOT NULL COMMENT 'OAuth provider: KAKAO, APPLE',
    provider_id VARCHAR(255) NOT NULL COMMENT 'OAuth provider user ID',
    nickname    VARCHAR(50)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_provider_provider_id (provider, provider_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
