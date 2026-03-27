-- Project + Requirement management tables (PostgreSQL)
-- Depends on: sys_user / sys_role / sys_permission / relation tables

CREATE TABLE IF NOT EXISTS pm_project (
    id             BIGSERIAL PRIMARY KEY,
    project_key    VARCHAR(64)  NOT NULL UNIQUE,
    project_name   VARCHAR(128) NOT NULL,
    description    TEXT         NULL,
    project_background TEXT     NULL,
    similar_products  TEXT      NULL,
    target_customer_groups VARCHAR(1000) NULL,
    commercial_value TEXT       NULL,
    core_product_value TEXT     NULL,
    business_knowledge_summary TEXT NULL,
    project_type   VARCHAR(32)  NULL,
    creation_mode  VARCHAR(32)  NOT NULL DEFAULT 'FORM',
    priority       VARCHAR(16)  NOT NULL DEFAULT 'P2',
    start_date     DATE         NULL,
    target_date    DATE         NULL,
    tags           VARCHAR(512) NULL,
    visibility     VARCHAR(32)  NOT NULL DEFAULT 'PRIVATE',
    status         VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    owner_user_id  BIGINT       NOT NULL REFERENCES sys_user (id),
    created_by     BIGINT       NOT NULL REFERENCES sys_user (id),
    updated_by     BIGINT       NOT NULL REFERENCES sys_user (id),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pm_project_owner ON pm_project (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_pm_project_status ON pm_project (status);

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
    project_id           BIGINT       NULL REFERENCES pm_project (id) ON DELETE SET NULL,
    session_id           BIGINT       NULL REFERENCES pm_project_chat_session (id) ON DELETE CASCADE,
    material_type        VARCHAR(16)  NOT NULL,
    title                VARCHAR(256) NULL,
    source_uri           VARCHAR(1000) NULL,
    raw_content          TEXT         NULL,
    ai_extracted_summary TEXT         NULL,
    created_by           BIGINT       NOT NULL REFERENCES sys_user (id),
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pm_project_source_material_project ON pm_project_source_material (project_id);
CREATE INDEX IF NOT EXISTS idx_pm_project_source_material_session ON pm_project_source_material (session_id);
CREATE INDEX IF NOT EXISTS idx_pm_project_source_material_type ON pm_project_source_material (material_type);

CREATE TABLE IF NOT EXISTS pm_project_member (
    id             BIGSERIAL PRIMARY KEY,
    project_id      BIGINT    NOT NULL REFERENCES pm_project (id) ON DELETE CASCADE,
    user_id         BIGINT    NOT NULL REFERENCES sys_user (id) ON DELETE CASCADE,
    project_role    VARCHAR(32) NOT NULL DEFAULT 'DEV',
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by      BIGINT    NOT NULL REFERENCES sys_user (id),
    updated_by      BIGINT    NOT NULL REFERENCES sys_user (id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (project_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_pm_project_member_project ON pm_project_member (project_id);
CREATE INDEX IF NOT EXISTS idx_pm_project_member_user ON pm_project_member (user_id);

CREATE TABLE IF NOT EXISTS rm_requirement (
    id               BIGSERIAL PRIMARY KEY,
    project_id        BIGINT       NOT NULL REFERENCES pm_project (id) ON DELETE CASCADE,
    requirement_no    VARCHAR(64)  NOT NULL,
    title             VARCHAR(256) NOT NULL,
    summary           TEXT         NULL,
    priority          VARCHAR(16)  NOT NULL DEFAULT 'P2',
    status            VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    assignee_user_id  BIGINT       NULL REFERENCES sys_user (id),
    reporter_user_id  BIGINT       NOT NULL REFERENCES sys_user (id),
    current_version_id BIGINT      NULL,
    created_by        BIGINT       NOT NULL REFERENCES sys_user (id),
    updated_by        BIGINT       NOT NULL REFERENCES sys_user (id),
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (project_id, requirement_no)
);

CREATE INDEX IF NOT EXISTS idx_rm_requirement_project ON rm_requirement (project_id);
CREATE INDEX IF NOT EXISTS idx_rm_requirement_status ON rm_requirement (status);
CREATE INDEX IF NOT EXISTS idx_rm_requirement_priority ON rm_requirement (priority);

CREATE TABLE IF NOT EXISTS rm_requirement_version (
    id                 BIGSERIAL PRIMARY KEY,
    requirement_id      BIGINT       NOT NULL REFERENCES rm_requirement (id) ON DELETE CASCADE,
    version_no          VARCHAR(32)  NOT NULL,
    prd_markdown        TEXT         NOT NULL,
    change_summary      TEXT         NULL,
    source_job_id       VARCHAR(64)  NULL,
    generated_by        BIGINT       NOT NULL REFERENCES sys_user (id),
    generated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_current          BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (requirement_id, version_no)
);

CREATE INDEX IF NOT EXISTS idx_rm_req_ver_req ON rm_requirement_version (requirement_id);
CREATE INDEX IF NOT EXISTS idx_rm_req_ver_current ON rm_requirement_version (requirement_id, is_current);

CREATE TABLE IF NOT EXISTS rm_requirement_chat_session (
    id                    BIGSERIAL PRIMARY KEY,
    requirement_id         BIGINT       NOT NULL REFERENCES rm_requirement (id) ON DELETE CASCADE,
    job_id                 VARCHAR(64)  NOT NULL UNIQUE,
    status                 VARCHAR(32)  NOT NULL,
    pending_question       TEXT         NULL,
    confirmed_items_json   TEXT         NULL,
    unconfirmed_items_json TEXT         NULL,
    ready_to_generate      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by             BIGINT       NOT NULL REFERENCES sys_user (id),
    created_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rm_chat_session_req ON rm_requirement_chat_session (requirement_id);
CREATE INDEX IF NOT EXISTS idx_rm_chat_session_status ON rm_requirement_chat_session (status);

CREATE TABLE IF NOT EXISTS rm_requirement_chat_message (
    id             BIGSERIAL PRIMARY KEY,
    session_id      BIGINT       NOT NULL REFERENCES rm_requirement_chat_session (id) ON DELETE CASCADE,
    role            VARCHAR(16)  NOT NULL,
    content         TEXT         NOT NULL,
    seq_no          INT          NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (session_id, seq_no)
);

CREATE INDEX IF NOT EXISTS idx_rm_chat_msg_session ON rm_requirement_chat_message (session_id);
CREATE INDEX IF NOT EXISTS idx_rm_chat_msg_role ON rm_requirement_chat_message (role);

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS km_document (
    id                 BIGSERIAL PRIMARY KEY,
    project_id         BIGINT       NULL REFERENCES pm_project (id) ON DELETE CASCADE,
    requirement_id     BIGINT       NULL REFERENCES rm_requirement (id) ON DELETE CASCADE,
    source_material_id BIGINT       NULL REFERENCES pm_project_source_material (id) ON DELETE SET NULL,
    document_type      VARCHAR(16)  NOT NULL,
    source_uri         VARCHAR(1000) NULL,
    title              VARCHAR(256) NULL,
    status             VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    raw_text           TEXT         NULL,
    clean_text         TEXT         NULL,
    summary            TEXT         NULL,
    keywords_json      TEXT         NULL,
    tags               VARCHAR(512) NULL,
    content_hash       VARCHAR(64)  NULL,
    version_no         INT          NOT NULL DEFAULT 1,
    is_latest          BOOLEAN      NOT NULL DEFAULT TRUE,
    retrievable        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by         BIGINT       NOT NULL REFERENCES sys_user (id),
    updated_by         BIGINT       NOT NULL REFERENCES sys_user (id),
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_km_document_project ON km_document (project_id);
CREATE INDEX IF NOT EXISTS idx_km_document_requirement ON km_document (requirement_id);
CREATE INDEX IF NOT EXISTS idx_km_document_source_material ON km_document (source_material_id);
CREATE INDEX IF NOT EXISTS idx_km_document_status ON km_document (status);
CREATE INDEX IF NOT EXISTS idx_km_document_type ON km_document (document_type);
CREATE INDEX IF NOT EXISTS idx_km_document_retrievable ON km_document (retrievable);
CREATE INDEX IF NOT EXISTS idx_km_document_hash ON km_document (content_hash);
CREATE INDEX IF NOT EXISTS idx_km_document_latest ON km_document (project_id, requirement_id, is_latest);

CREATE TABLE IF NOT EXISTS km_document_asset (
    id             BIGSERIAL PRIMARY KEY,
    document_id    BIGINT       NOT NULL REFERENCES km_document (id) ON DELETE CASCADE,
    asset_role     VARCHAR(32)  NOT NULL,
    storage_bucket VARCHAR(128) NOT NULL,
    storage_key    VARCHAR(512) NOT NULL,
    mime_type      VARCHAR(128) NULL,
    size_bytes     BIGINT       NULL,
    sha256         VARCHAR(64)  NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_km_doc_asset_document ON km_document_asset (document_id);
CREATE INDEX IF NOT EXISTS idx_km_doc_asset_role ON km_document_asset (asset_role);
CREATE INDEX IF NOT EXISTS idx_km_doc_asset_sha256 ON km_document_asset (sha256);
CREATE UNIQUE INDEX IF NOT EXISTS uq_km_doc_asset_doc_role_key
    ON km_document_asset (document_id, asset_role, storage_key);

CREATE TABLE IF NOT EXISTS km_document_chunk (
    id               BIGSERIAL PRIMARY KEY,
    document_id      BIGINT       NOT NULL REFERENCES km_document (id) ON DELETE CASCADE,
    chunk_no         INT          NOT NULL,
    chunk_text       TEXT         NOT NULL,
    token_count      INT          NULL,
    summary          TEXT         NULL,
    embedding_status VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    vector_ref       VARCHAR(128) NULL,
    embedding_model  VARCHAR(64)  NULL,
    embedding_vector vector(1536) NULL,
    embedded_at      TIMESTAMP    NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (document_id, chunk_no)
);

CREATE INDEX IF NOT EXISTS idx_km_doc_chunk_document ON km_document_chunk (document_id);
CREATE INDEX IF NOT EXISTS idx_km_doc_chunk_embedding_status ON km_document_chunk (embedding_status);
CREATE INDEX IF NOT EXISTS idx_km_doc_chunk_vector_ref ON km_document_chunk (vector_ref);
CREATE INDEX IF NOT EXISTS idx_km_doc_chunk_doc_status ON km_document_chunk (document_id, embedding_status);
CREATE INDEX IF NOT EXISTS idx_km_doc_chunk_vector_ivfflat
    ON km_document_chunk USING ivfflat (embedding_vector vector_cosine_ops)
    WITH (lists = 100);

CREATE TABLE IF NOT EXISTS km_document_task (
    id            BIGSERIAL PRIMARY KEY,
    document_id   BIGINT       NOT NULL REFERENCES km_document (id) ON DELETE CASCADE,
    task_type     VARCHAR(32)  NOT NULL,
    status        VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    attempt_count INT          NOT NULL DEFAULT 0,
    last_error    TEXT         NULL,
    started_at    TIMESTAMP    NULL,
    finished_at   TIMESTAMP    NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_km_doc_task_document ON km_document_task (document_id);
CREATE INDEX IF NOT EXISTS idx_km_doc_task_status ON km_document_task (status);
CREATE INDEX IF NOT EXISTS idx_km_doc_task_type ON km_document_task (task_type);
CREATE INDEX IF NOT EXISTS idx_km_doc_task_doc_status ON km_document_task (document_id, status);

ALTER TABLE rm_requirement
    ADD CONSTRAINT fk_rm_requirement_current_version
        FOREIGN KEY (current_version_id) REFERENCES rm_requirement_version (id) DEFERRABLE INITIALLY DEFERRED;

-- Optional seed permissions
INSERT INTO sys_permission (perm_code, perm_name, status)
VALUES
    ('PROJECT:CREATE', 'Create Project', 'ENABLED'),
    ('PROJECT:EDIT', 'Edit Project', 'ENABLED'),
    ('PROJECT:ARCHIVE', 'Archive Project', 'ENABLED'),
    ('REQUIREMENT:CREATE', 'Create Requirement', 'ENABLED'),
    ('REQUIREMENT:EDIT', 'Edit Requirement', 'ENABLED'),
    ('REQUIREMENT:AI_CHAT', 'AI Clarify Requirement', 'ENABLED'),
    ('REQUIREMENT:GENERATE_PRD', 'Generate PRD', 'ENABLED'),
    ('REQUIREMENT:VERSION_VIEW', 'View Requirement Versions', 'ENABLED')
ON CONFLICT (perm_code) DO NOTHING;
