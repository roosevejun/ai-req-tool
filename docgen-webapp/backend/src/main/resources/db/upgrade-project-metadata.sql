-- Upgrade: extend pm_project with richer metadata fields (PostgreSQL)
-- Safe to run multiple times.

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS project_type VARCHAR(32);

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS priority VARCHAR(16) NOT NULL DEFAULT 'P2';

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS start_date DATE;

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS target_date DATE;

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS tags VARCHAR(512);

