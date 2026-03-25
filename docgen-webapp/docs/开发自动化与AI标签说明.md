# 开发自动化与 AI 标签维护说明

本文档说明两项新增能力：

1. 开发自动化脚本（提交 + Docker 重建重启）
2. 项目标签由 AI 自动维护

---

## 1) 自动化脚本：`scripts/dev-rebuild.ps1`

### 功能

脚本会按顺序执行：

1. 在 `docgen-webapp` 目录范围内自动暂存代码变更（过滤 `.m2`、`scripts/artifacts`、`node_modules`、`dist`）
2. 自动 `git commit`
3. 后端执行 `docker compose down -> build -> up -d -> ps`
4. 前端执行 `docker compose down -> build -> up -d -> ps`

### 常用命令

在项目根目录执行：

```powershell
cd g:\Agent\docgen-webapp
```

1. 预演（不执行，仅打印步骤）

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev-rebuild.ps1 -DryRun
```

2. 正式执行（自动提交 + 重建重启）

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev-rebuild.ps1 -CommitMessage "chore: auto rebuild"
```

3. 只做 Docker 重建重启，不提交

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev-rebuild.ps1 -SkipCommit
```

4. 只重建后端 / 只重建前端

```powershell
# 只后端
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev-rebuild.ps1 -SkipFrontend

# 只前端
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev-rebuild.ps1 -SkipBackend
```

### 参数说明

- `-CommitMessage`：提交信息
- `-SkipCommit`：跳过提交
- `-SkipBackend`：跳过后端 Docker 周期
- `-SkipFrontend`：跳过前端 Docker 周期
- `-NoCache`：构建时加 `--no-cache`（默认开启）
- `-DryRun`：仅打印命令，不执行

---

## 2) 项目扩展字段

项目表 `pm_project` 已支持以下字段：

- `project_type`
- `priority`
- `start_date`
- `target_date`
- `tags`

增量升级脚本：

- `backend/src/main/resources/db/upgrade-project-metadata.sql`

---

## 3) AI 自动维护标签（tags）

### 触发时机

在需求工作台中生成 PRD 成功后自动触发。

### 处理流程

1. 后端调用 `AgentClient.suggestProjectTags(...)` 做标签建议  
2. 优先使用 LLM 返回结构化 JSON：`{"tags":[...]}`  
3. 若 LLM 失败或空结果，自动回退到关键词规则提取  
4. 调用 `ProjectService.mergeTagsByAi(...)` 合并写回项目 `tags`

### 合并规则

- 保留已有标签
- 新标签去重后合并
- 最多保留 20 个标签
- 存储字段最大长度 512（超长会截断）

---

## 4) 相关代码位置

- 自动化脚本：`scripts/dev-rebuild.ps1`
- 标签建议：`backend/src/main/java/com/tongtu/docgen/llm/AgentClient.java`
- 标签回写：`backend/src/main/java/com/tongtu/docgen/project/service/ProjectService.java`
- PRD 生成后触发：`backend/src/main/java/com/tongtu/docgen/project/service/RequirementDocgenService.java`

---

## 5) 常见问题

### Q1: 页面看起来还是旧版？

请先执行脚本重建前端，或手工执行：

```powershell
docker compose -f .\frontend\docker-compose.yml down
docker compose -f .\frontend\docker-compose.yml build --no-cache
docker compose -f .\frontend\docker-compose.yml up -d
```

然后浏览器 `Ctrl + F5` 强刷。

### Q2: 标签为什么没变化？

请确认本次流程走到了“生成 PRD 成功”；仅澄清对话未生成时不会触发标签回写。

