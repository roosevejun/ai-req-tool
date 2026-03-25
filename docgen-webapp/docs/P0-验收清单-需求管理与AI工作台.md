# P0 验收清单（需求管理与 AI 工作台）

适用范围：
- 后端：`docgen-webapp/backend`
- 前端：`docgen-webapp/frontend`
- 数据库：PostgreSQL（`ai-req-tool`）

目标：
- 验证「登录 -> 项目 -> 需求 -> AI 对话 -> 生成 PRD -> 版本导出」闭环可用。
- 验证 AI 会话持久化是否真正落库，不依赖内存态。

---

## 1. 环境准备

- [ ] 后端启动成功（`http://localhost:8080`）
- [ ] 前端启动成功（`http://localhost:5173`）
- [ ] 已执行初始化 SQL：
  - [ ] `backend/src/main/resources/db/init-system.sql`
  - [ ] `backend/src/main/resources/db/init-project-requirement.sql`
- [ ] 测试账号可登录（默认）：
  - 用户名：`admin`
  - 密码：`Admin@123`

---

## 2. 鉴权与权限

- [ ] `POST /api/system/auth/login` 返回 `token`
- [ ] 不带 token 访问受保护接口返回 `401`
- [ ] 普通受保护接口（如 `/api/projects`）携带 token 返回 `200`
- [ ] 管理员角色可通过 `@RequiredPermission` 保护接口（ADMIN 旁路）

---

## 3. 项目与需求

- [ ] 创建项目：`POST /api/projects` 成功返回 `projectId`
- [ ] 项目列表：`GET /api/projects` 可查到新建项目
- [ ] 创建需求：`POST /api/projects/{projectId}/requirements` 成功返回 `requirementId`
- [ ] 需求列表：`GET /api/projects/{projectId}/requirements` 可查到新建需求

---

## 4. AI 工作台（核心）

- [ ] 创建会话：`POST /api/requirements/{requirementId}/docgen/jobs`
  - [ ] 返回 `jobId`
  - [ ] `rm_requirement_chat_session` 落库一条记录
  - [ ] `rm_requirement_chat_message` 至少有 1 条 user/assistant 初始化消息

- [ ] 对话澄清：`POST /api/requirements/{requirementId}/docgen/jobs/{jobId}/chat`
  - [ ] 返回 `confirmedItems / unconfirmedItems / pendingQuestion`
  - [ ] 新增聊天消息写入 `rm_requirement_chat_message`

- [ ] 生成 PRD：`POST /api/requirements/{requirementId}/docgen/jobs/{jobId}/generate`
  - [ ] 会话状态更新为 `COMPLETED`
  - [ ] 新增版本记录 `rm_requirement_version`
  - [ ] `rm_requirement.current_version_id` 更新

---

## 5. 版本与导出

- [ ] `GET /api/requirements/{id}/versions` 可看到新版本
- [ ] `GET /api/requirements/{id}/versions/{versionId}` 可查看正文
- [ ] `GET /api/requirements/{id}/versions/{versionId}/export` 下载成功（markdown）
- [ ] `GET /api/requirements/{requirementId}/docgen/jobs/{jobId}/export` 下载成功

---

## 6. 持久化与恢复验证（P0 必做）

- [ ] 在完成一次 chat 后重启后端
- [ ] 重启后调用 `GET /api/requirements/{requirementId}/docgen/jobs/{jobId}`
  - [ ] 能恢复会话状态
  - [ ] 聊天历史可继续使用（不丢失）
- [ ] 再发一条 chat 并生成 PRD 成功

---

## 7. 建议执行方式

- 推荐直接运行脚本：`scripts/e2e-p0-check.ps1`
- 脚本会自动执行：
  - 登录
  - 创建项目
  - 创建需求
  - 创建 AI 会话
  - 对话
  - 生成 PRD
  - 查询版本并导出

