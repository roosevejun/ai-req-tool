ALTER TABLE rm_requirement_chat_session
    ADD COLUMN IF NOT EXISTS template_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS template_version_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS template_version_label VARCHAR(64) NULL,
    ADD COLUMN IF NOT EXISTS template_snapshot_markdown TEXT NULL,
    ADD COLUMN IF NOT EXISTS template_variables_json TEXT NULL;

CREATE INDEX IF NOT EXISTS idx_req_chat_session_template_id ON rm_requirement_chat_session(template_id);
