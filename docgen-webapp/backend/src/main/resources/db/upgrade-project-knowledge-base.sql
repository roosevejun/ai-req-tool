-- Upgrade: introduce mid-term project knowledge base schema (PostgreSQL)
-- Safe to run multiple times.
-- This file extends the current project source material model without breaking
-- existing pm_project_source_material usage.
-- New runtime services can migrate to km_* tables incrementally.

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
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (document_id, chunk_no)
);

CREATE INDEX IF NOT EXISTS idx_km_doc_chunk_document ON km_document_chunk (document_id);
CREATE INDEX IF NOT EXISTS idx_km_doc_chunk_embedding_status ON km_document_chunk (embedding_status);
CREATE INDEX IF NOT EXISTS idx_km_doc_chunk_vector_ref ON km_document_chunk (vector_ref);

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
