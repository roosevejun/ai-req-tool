# DocGen WebApp（AI 需求管理工具）

DocGen WebApp 是一个面向需求管理场景的 AI 工具，支持：

1. 对话澄清需求
2. 生成与修订 PRD（Markdown）
3. 项目管理 / 需求管理 / 版本管理
4. 用户、角色、权限（RBAC）管理
5. Swagger/OpenAPI 鉴权联动

---

## 目录结构

- `backend`：Spring Boot + MyBatis + PostgreSQL + Druid
- `frontend`：Vue 3 + Vite
- `k8s`：Kubernetes 部署清单
- `scripts`：自动化脚本（E2E、重建发布等）
- `docs`：项目文档

---

## 快速启动（本地）

### 1) 初始化数据库

在 PostgreSQL 中执行：

1. `backend/src/main/resources/db/init-system.sql`
2. `backend/src/main/resources/db/init-project-requirement.sql`
3. （增量升级）`backend/src/main/resources/db/upgrade-project-metadata.sql`

### 2) 启动后端

```powershell
cd g:\Agent\docgen-webapp\backend
mvn spring-boot:run
```

后端地址：`http://localhost:8080`

Swagger：`http://localhost:8080/swagger-ui/index.html`

### 3) 启动前端

```powershell
cd g:\Agent\docgen-webapp\frontend
npm install
npm run dev
```

前端地址：`http://localhost:5173`

---

## Docker（前后端独立 compose）

### 后端

```powershell
cd g:\Agent\docgen-webapp\backend
docker compose up -d --build
```

### 前端

```powershell
cd g:\Agent\docgen-webapp\frontend
docker compose up -d --build
```

---

## 一键自动化脚本

脚本：`scripts/dev-rebuild.ps1`

功能：自动执行「提交代码 + Docker down/build/up + 状态检查」。

```powershell
cd g:\Agent\docgen-webapp
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev-rebuild.ps1 -CommitMessage "chore: auto rebuild"
```

仅预演：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev-rebuild.ps1 -DryRun
```

---

## AI 自动维护项目标签（tags）

在需求工作台生成 PRD 成功后，后端会自动提取标签并合并到项目 `tags` 字段。

- LLM 结构化提取优先
- 本地规则提取兜底
- 自动去重与长度控制

详见：`docs/开发自动化与AI标签说明.md`

---

## 其他文档

- 部署指南：`README-部署指南.md`
- K8s 部署：`k8s/README.md`
- OpenAPI 鉴权：`backend/OPENAPI-AUTH-GUIDE.md`
- 前后端联调清单：`docs/前后端联调验收清单.md`

