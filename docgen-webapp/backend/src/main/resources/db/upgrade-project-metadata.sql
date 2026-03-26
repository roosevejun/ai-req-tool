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
    ADD COLUMN IF NOT EXISTS project_type VARCHAR(32);

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS priority VARCHAR(16) NOT NULL DEFAULT 'P2';

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS start_date DATE;

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS target_date DATE;

ALTER TABLE IF EXISTS pm_project
    ADD COLUMN IF NOT EXISTS tags VARCHAR(512);

