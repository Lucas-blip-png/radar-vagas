CREATE TABLE job (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    source       VARCHAR(30)  NOT NULL,
    url          VARCHAR(500) NOT NULL,
    title        VARCHAR(300) NOT NULL,
    company      VARCHAR(200),
    location     VARCHAR(200),
    tags         VARCHAR(500),
    published_at TIMESTAMP(6) WITH TIME ZONE,
    score        INT          NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    applied_at   TIMESTAMP(6) WITH TIME ZONE,
    follow_up_on DATE,
    notes        VARCHAR(1000),
    found_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_job_url UNIQUE (url)
);

CREATE INDEX idx_job_status_score ON job (status, score);
