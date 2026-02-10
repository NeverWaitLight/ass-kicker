-- Migration script to create core tables (PostgreSQL)

CREATE TABLE t_template (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    created_at BIGINT NOT NULL,
    updated_at BIGINT,
    CONSTRAINT uk_t_template_code UNIQUE (code)
);

CREATE INDEX idx_t_template_code ON t_template (code);

CREATE TABLE t_language_template (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    language VARCHAR(10) NOT NULL,
    content TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT,
    CONSTRAINT fk_t_language_template_template_id FOREIGN KEY (template_id) REFERENCES t_template(id) ON DELETE CASCADE,
    CONSTRAINT uk_t_language_template_template_language UNIQUE (template_id, language)
);

CREATE INDEX idx_t_language_template_template_id ON t_language_template (template_id);
CREATE INDEX idx_t_language_template_language ON t_language_template (language);