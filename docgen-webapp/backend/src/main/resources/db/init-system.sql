-- System management module initialization (PostgreSQL)
-- Run this script once on database: ai-req-tool

CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(128) NOT NULL,
    status        VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sys_role (
    id         BIGSERIAL PRIMARY KEY,
    role_code  VARCHAR(64)  NOT NULL UNIQUE,
    role_name  VARCHAR(128) NOT NULL,
    status     VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id         BIGSERIAL PRIMARY KEY,
    perm_code  VARCHAR(64)  NOT NULL UNIQUE,
    perm_name  VARCHAR(128) NOT NULL,
    status     VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL REFERENCES sys_user (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES sys_role (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id       BIGINT NOT NULL REFERENCES sys_role (id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES sys_permission (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

INSERT INTO sys_role (role_code, role_name, status)
VALUES ('ADMIN', 'System Administrator', 'ENABLED')
ON CONFLICT (role_code) DO NOTHING;

INSERT INTO sys_permission (perm_code, perm_name, status)
VALUES ('SYSTEM:MANAGE', 'System Management', 'ENABLED')
ON CONFLICT (perm_code) DO NOTHING;

INSERT INTO sys_user (username, password_hash, display_name, status)
VALUES ('admin', '{noop}Admin@123', 'Administrator', 'ENABLED')
ON CONFLICT (username) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
         JOIN sys_role r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         JOIN sys_permission p ON p.perm_code = 'SYSTEM:MANAGE'
WHERE r.role_code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

