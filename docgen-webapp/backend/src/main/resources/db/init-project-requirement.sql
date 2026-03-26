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
    project_type   VARCHAR(32)  NULL,
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
