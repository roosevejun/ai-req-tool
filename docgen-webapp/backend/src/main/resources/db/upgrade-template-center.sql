-- Upgrade: introduce system template center schema (PostgreSQL)
-- Safe to run multiple times.

CREATE TABLE IF NOT EXISTS sys_template (
    id                 BIGSERIAL PRIMARY KEY,
    template_code      VARCHAR(64)   NOT NULL UNIQUE,
    template_name      VARCHAR(128)  NOT NULL,
    template_category  VARCHAR(32)   NOT NULL DEFAULT 'PRD',
    description        TEXT          NULL,
    scope_level        VARCHAR(32)   NOT NULL DEFAULT 'SYSTEM',
    status             VARCHAR(32)   NOT NULL DEFAULT 'DRAFT',
    latest_version_no  INT           NOT NULL DEFAULT 0,
    published_version_no INT         NULL,
    created_by         BIGINT        NOT NULL REFERENCES sys_user (id),
    updated_by         BIGINT        NOT NULL REFERENCES sys_user (id),
    created_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sys_template_category ON sys_template (template_category);
CREATE INDEX IF NOT EXISTS idx_sys_template_scope ON sys_template (scope_level);
CREATE INDEX IF NOT EXISTS idx_sys_template_status ON sys_template (status);

CREATE TABLE IF NOT EXISTS sys_template_version (
    id               BIGSERIAL PRIMARY KEY,
    template_id      BIGINT        NOT NULL REFERENCES sys_template (id) ON DELETE CASCADE,
    version_no       INT           NOT NULL,
    version_label    VARCHAR(64)   NULL,
    content_markdown TEXT          NOT NULL,
    variables_json   TEXT          NULL,
    notes            TEXT          NULL,
    status           VARCHAR(32)   NOT NULL DEFAULT 'DRAFT',
    is_published     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_by       BIGINT        NOT NULL REFERENCES sys_user (id),
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (template_id, version_no)
);

CREATE INDEX IF NOT EXISTS idx_sys_template_version_template ON sys_template_version (template_id);
CREATE INDEX IF NOT EXISTS idx_sys_template_version_status ON sys_template_version (status);
CREATE INDEX IF NOT EXISTS idx_sys_template_version_publish ON sys_template_version (template_id, is_published);

INSERT INTO sys_permission (perm_code, perm_name, status)
VALUES ('TEMPLATE:MANAGE', 'Template Center Management', 'ENABLED')
ON CONFLICT (perm_code) DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.perm_code = 'TEMPLATE:MANAGE'
WHERE r.role_code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;
