# DocGen WebApp（AI 需求管理工具）

这是一个面向需求管理场景的 AI 工具，支持：

1. 通过对话收集/澄清需求。
2. 生成与迭代 PRD Markdown 文档。
3. 项目管理、需求管理、版本管理。
4. 用户登录、用户/角色/权限管理（RBAC）。
5. Swagger/OpenAPI 与权限联动展示。

---

## 目录结构

- `backend`：Spring Boot + MyBatis + PostgreSQL + Druid
- `frontend`：Vue 3 + Vite

---

## 核心能力

### 1) AI 需求工作台

- 创建会话并进入澄清阶段
- 逐步对话补齐未确认项
- 生成 PRD 并保存版本
- 导出指定 PRD Markdown

### 2) 系统管理（RBAC）

- 用户管理：创建、更新、重置密码、绑定角色
- 角色管理：创建、更新、绑定权限
- 权限管理：创建、更新、列表

### 3) 项目与需求管理

- 项目：列表、详情、创建、更新
- 需求：创建、编辑、版本查看、版本导出

### 4) OpenAPI 权限联动

- 默认接口要求 Bearer Token
- `@PublicApi` 接口自动标记为公开
- `@RequiredPermission` 自动写入 `x-permissions`
- 自动补充 `401/403` 响应说明

详细说明见：
- `backend/OPENAPI-AUTH-GUIDE.md`

---

## 环境要求

- JDK 17
- Maven 3.9+
- Node.js 18+
- PostgreSQL 14+（推荐）

---

## 数据库初始化

默认数据库名建议：`ai-req-tool`

在 PostgreSQL 中依次执行：

1. `backend/src/main/resources/db/init-system.sql`
2. `backend/src/main/resources/db/init-project-requirement.sql`

初始化后会生成默认管理员账号：

- 用户名：`admin`
- 密码：`Admin@123`（当前脚本为 `{noop}` 明文方式，仅用于开发环境）

---

## 后端启动

在 `g:\Agent\docgen-webapp\backend` 下执行：

```powershell
mvn spring-boot:run
```

默认地址：

- `http://localhost:8080`

Swagger UI：

- `http://localhost:8080/swagger-ui/index.html`

---

## 前端启动

在 `g:\Agent\docgen-webapp\frontend` 下执行：

```powershell
npm install
npm run dev
```

默认地址：

- `http://localhost:5173`

---

## LLM 配置

`application.yml` 当前读取优先级：

1. `AGENT_ROUTER_TOKEN`
2. `OPENAI_API_KEY`

即：

```yaml
agent:
  llm:
    openai:
      apiKey: ${AGENT_ROUTER_TOKEN:${OPENAI_API_KEY:}}
```

### 推荐方式：环境变量

PowerShell：

```powershell
$env:OPENAI_API_KEY="your-key"
```

或：

```powershell
$env:AGENT_ROUTER_TOKEN="your-token"
```

### 本地配置文件（避免提交密钥）

复制：

- `backend/src/main/resources/application-local.yml.example`
- 到 `backend/src/main/resources/application-local.yml`

并填入本地 key。请勿提交真实密钥到仓库。

---

## WSL 联调说明（Windows 后端 + WSL 前端）

如果前端在 WSL 里运行，访问 `127.0.0.1:8080` 可能失败（`ECONNREFUSED`）。

可按以下步骤处理：

1. 在 WSL 中获取 Windows 主机 IP：

```bash
ip route | grep default | awk '{print $3}'
```

2. 测试后端连通性：

```bash
curl http://<windows-host-ip>:8080/api/docgen/health
```

3. 前端设置代理目标：

```bash
export VITE_BACKEND_URL="http://<windows-host-ip>:8080"
npm run dev
```

---

## 测试

后端测试：

```powershell
cd g:\Agent\docgen-webapp\backend
mvn test
```

当前已包含 OpenAPI 权限联动的单元测试（`OpenApiConfigTest`）。

---

## 安全注意事项

1. 不要在 `application.yml`、`application-local.yml`、README 中写入真实密钥。
2. 默认管理员密码仅用于开发环境，部署前请修改并启用安全加密策略。
3. 生产环境请替换 `agent.auth.token.secret`，不要使用默认值。

---

## 更多文档

1. 部署指南：`README-部署指南.md`
2. OpenAPI 与权限联动：`backend/OPENAPI-AUTH-GUIDE.md`
3. 前后端联调验收清单：`docs/前后端联调验收清单.md`
4. 项目/需求模块回滚脚本：`backend/src/main/resources/db/rollback-project-requirement.sql`
