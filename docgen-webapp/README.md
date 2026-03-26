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
3. 如果数据库是老库，再执行增量升级脚本：`backend/src/main/resources/db/upgrade-project-metadata.sql`

说明：

1. 新库初始化：执行前两个 `init-*.sql` 即可。
2. 老库升级：在原有库结构基础上，额外执行 `upgrade-project-metadata.sql`。
3. 本次升级会补齐项目产品化相关字段，包括：
   - `project_background`
   - `similar_products`
   - `target_customer_groups`
   - `commercial_value`
   - `core_product_value`
   - 以及已有的 `project_type / priority / start_date / target_date / tags`
4. 从当前版本开始，后端启动时会自动扫描 `backend/src/main/resources/db/upgrade-*.sql`。
5. 系统会将已执行脚本及其校验值记录到数据库表 `sys_schema_upgrade`。
6. 如果升级脚本内容发生变化，系统会重新执行该脚本，因此升级 SQL 必须保持幂等。

### 2) 启动后端

```powershell
cd g:\Agent\docgen-webapp\backend
mvn spring-boot:run
```

如需临时关闭自动升级：

```powershell
$env:DB_AUTO_UPGRADE_ENABLED = "false"
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
cd C:\javaDevelop\ai-req-tool\docgen-webapp
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev-rebuild.ps1 -CommitMessage "chore: auto rebuild"
```

说明：

1. 脚本默认会自动解析 `ProjectRoot`，优先使用当前目录，其次回退到脚本所在目录的上一级（即 `docgen-webapp`）。
2. 如果你不是在 `docgen-webapp` 目录下执行，可以显式传入 `-ProjectRoot`。
3. 脚本默认使用 Docker 缓存构建，避免在网络受限时频繁重新拉取基础镜像。
4. 如果你确实需要强制全量重建，可以显式加 `-NoCache`。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev-rebuild.ps1 -ProjectRoot C:\javaDevelop\ai-req-tool\docgen-webapp
```

强制无缓存构建：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev-rebuild.ps1 -NoCache
```

仅预演：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev-rebuild.ps1 -DryRun
```

---

## Docker 构建并推送 Harbor

脚本：`scripts/docker-build-push.ps1`

功能：自动执行「Harbor 登录 + backend 镜像构建与推送 + frontend 镜像构建与推送」。

推荐先准备本地环境变量，避免把账号密码写进命令历史：

```powershell
$env:HARBOR_USERNAME = "your-username"
$env:HARBOR_PASSWORD = "your-password"
```

默认推送目标：

- `harbor.ahtongtu.cn/ai-req-tool/backend:<tag>`
- `harbor.ahtongtu.cn/ai-req-tool/frontend:<tag>`
- 默认同时推送：
  `harbor.ahtongtu.cn/ai-req-tool/backend:latest`
  `harbor.ahtongtu.cn/ai-req-tool/frontend:latest`

其中 `<tag>` 默认取当前 git 短提交号，也可以显式指定。

示例：

```powershell
cd C:\javaDevelop\ai-req-tool\docgen-webapp
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\docker-build-push.ps1
```

显式指定参数：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\docker-build-push.ps1 `
  -Registry harbor.ahtongtu.cn `
  -Repository ai-req-tool `
  -Username $env:HARBOR_USERNAME `
  -Password $env:HARBOR_PASSWORD `
  -Tag v0.1.0
```

仅预演：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\docker-build-push.ps1 -DryRun
```

可参考示例环境文件：

- `harbor.env.example`

如果你只想推版本标签，不推 `latest`：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\docker-build-push.ps1 -SkipLatest
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

