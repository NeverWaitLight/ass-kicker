-- Migration script to create user tables and seed admin account

CREATE TABLE t_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT,
    last_login_at BIGINT,
    CONSTRAINT uk_t_user_username UNIQUE (username)
);

CREATE INDEX idx_t_user_username ON t_user (username);
CREATE INDEX idx_t_user_role ON t_user (role);
CREATE INDEX idx_t_user_status ON t_user (status);

INSERT INTO t_user (username, password_hash, role, status, created_at, updated_at)
VALUES (
    'admin',
    '.Da8CJI4.Z/FK2K7XK',
    'ADMIN',
    'ACTIVE',
    (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);
