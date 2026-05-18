CREATE TABLE toilets
(
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255)  NOT NULL COMMENT '화장실명',
    address     VARCHAR(500)  NOT NULL COMMENT '주소',
    lat         DOUBLE        NOT NULL COMMENT '위도',
    lng         DOUBLE        NOT NULL COMMENT '경도',
    is_public   TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '공중화장실 여부',
    male        TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '남성 사용 가능',
    female      TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '여성 사용 가능',
    disabled    TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '장애인 사용 가능',
    reported_by BIGINT                 COMMENT '제보한 사용자 ID (users.id)',
    status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'ACTIVE, PENDING, REJECTED',
    created_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_toilets_location (lat, lng),
    CONSTRAINT fk_toilets_reported_by FOREIGN KEY (reported_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
