-- Rollback script for project + requirement module (PostgreSQL)
-- This script rolls back objects created by init-project-requirement.sql
-- It intentionally keeps system management tables (sys_*) untouched.

BEGIN;

-- 1) Drop FK from rm_requirement -> rm_requirement_version first
ALTER TABLE IF EXISTS rm_requirement
    DROP CONSTRAINT IF EXISTS fk_rm_requirement_current_version;

-- 2) Drop child tables first
DROP TABLE IF EXISTS rm_requirement_chat_message;
DROP TABLE IF EXISTS rm_requirement_chat_session;
DROP TABLE IF EXISTS rm_requirement_version;
DROP TABLE IF EXISTS rm_requirement;
DROP TABLE IF EXISTS pm_project_member;
DROP TABLE IF EXISTS pm_project;

-- 3) Optional: remove seeded permissions from this module
DELETE FROM sys_permission
WHERE perm_code IN (
    'PROJECT:CREATE',
    'PROJECT:EDIT',
    'PROJECT:ARCHIVE',
    'REQUIREMENT:CREATE',
    'REQUIREMENT:EDIT',
    'REQUIREMENT:AI_CHAT',
    'REQUIREMENT:GENERATE_PRD',
    'REQUIREMENT:VERSION_VIEW'
);

COMMIT;

