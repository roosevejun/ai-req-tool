-- Upgrade: extend pm_project with richer metadata fields (PostgreSQL)
-- Safe to run multiple times.
-- Use this for existing databases that were initialized before the new project product fields were added.
-- New databases should still be created from init-project-requirement.sql.
-- This file is auto-detected and auto-applied by backend startup when DB auto upgrade is enabled.

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS project_background TEXT;

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS similar_products TEXT;

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS target_customer_groups VARCHAR(1000);

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS commercial_value TEXT;

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS core_product_value TEXT;

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS business_knowledge_summary TEXT;

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS project_type VARCHAR(32);

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS creation_mode VARCHAR(32) NOT NULL DEFAULT 'FORM';

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS priority VARCHAR(16) NOT NULL DEFAULT 'P2';

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS start_date DATE;

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS target_date DATE;

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS tags VARCHAR(512);

CREATE TABLE IF NOT EXISTS pm_project_chat_session (
    id                         BIGSERIAL PRIMARY KEY,
    project_id                 BIGINT       NULL REFERENCES pm_project (id) ON DELETE SET NULL,
    job_id                     VARCHAR(64)  NOT NULL UNIQUE,
    status                     VARCHAR(32)  NOT NULL,
    assistant_summary          TEXT         NULL,
    business_knowledge_summary TEXT         NULL,
    structured_info_json       TEXT         NULL,
    ready_to_create            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by                 BIGINT       NOT NULL REFERENCES sys_user (id),
    created_at                 TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pm_project_chat_session_project ON pm_project_chat_session (project_id);
CREATE INDEX IF NOT EXISTS idx_pm_project_chat_session_status ON pm_project_chat_session (status);
CREATE INDEX IF NOT EXISTS idx_pm_project_chat_session_created_by ON pm_project_chat_session (created_by);

CREATE TABLE IF NOT EXISTS pm_project_chat_message (
    id             BIGSERIAL PRIMARY KEY,
    session_id     BIGINT       NOT NULL REFERENCES pm_project_chat_session (id) ON DELETE CASCADE,
    seq_no         INT          NOT NULL,
    role           VARCHAR(16)  NOT NULL,
    message_type   VARCHAR(32)  NOT NULL,
    content        TEXT         NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (session_id, seq_no)
);

CREATE INDEX IF NOT EXISTS idx_pm_project_chat_msg_session ON pm_project_chat_message (session_id);
CREATE INDEX IF NOT EXISTS idx_pm_project_chat_msg_role ON pm_project_chat_message (role);

CREATE TABLE IF NOT EXISTS pm_project_source_material (
    id                   BIGSERIAL PRIMARY KEY,
    project_id           BIGINT        NULL REFERENCES pm_project (id) ON DELETE SET NULL,
    session_id           BIGINT        NULL REFERENCES pm_project_chat_session (id) ON DELETE CASCADE,
    material_type        VARCHAR(16)   NOT NULL,
    title                VARCHAR(256)  NULL,
    source_uri           VARCHAR(1000) NULL,
    raw_content          TEXT          NULL,
    ai_extracted_summary TEXT          NULL,
    created_by           BIGINT        NOT NULL REFERENCES sys_user (id),
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pm_project_source_material_project ON pm_project_source_material (project_id);
CREATE INDEX IF NOT EXISTS idx_pm_project_source_material_session ON pm_project_source_material (session_id);
CREATE INDEX IF NOT EXISTS idx_pm_project_source_material_type ON pm_project_source_material (material_type);

