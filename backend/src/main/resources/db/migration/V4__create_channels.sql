-- Migration script to create channel table (PostgreSQL)

CREATE TABLE t_channel (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    properties TEXT NOT NULL DEFAULT '{}',
    created_at BIGINT NOT NULL,
    updated_at BIGINT
);

CREATE INDEX idx_t_channel_name ON t_channel (name);
CREATE INDEX idx_t_channel_type ON t_channel (type);
