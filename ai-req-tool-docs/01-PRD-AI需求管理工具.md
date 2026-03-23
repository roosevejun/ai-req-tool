# PRD：AI需求管理工具（与 docgen-webapp 对齐版）

## 1. 产品定位
面向产品/研发团队的 AI 需求整理工具。用户输入业务描述后，系统通过多轮澄清形成结构化信息，再输出 PRD Markdown 并支持导出。

## 2. 业务目标
- 将需求从“口头描述”转为“可交付文档”。
- 把关键不确定项沉淀为 `unconfirmedItems` 并逐条清空。
- 在 `READY` 状态下触发生成，得到可评审 PRD。

## 3. 核心对象（按现有接口）
- `jobId`：任务唯一标识。
- `status`：`PENDING` / `CLARIFYING` / `READY` / `COMPLETED`。
- `chatHistory`：对话历史（`role` + `content`）。
- `confirmedItems`：已确认项。
- `unconfirmedItems`：待确认项。
- `pendingQuestion`：当前优先补充问题。
- `prdMarkdown`：最终文档。
- `basePrdMarkdown`：旧版基线文档（可为空）。

## 4. 用户流程（与接口一致）
1. 用户调用创建任务接口，提交 `businessDescription`（可带 `previousPrdMarkdown`）。
2. 系统返回 `jobId` 与初始 AI 回复，进入 `CLARIFYING` 或 `READY`。
3. 用户持续通过聊天接口补充信息，系统更新确认清单。
4. 当 `unconfirmedItems` 清空，状态进入 `READY`。
5. 用户调用生成接口，状态转为 `COMPLETED`，返回 `prdMarkdown`。
6. 用户调用导出接口下载 `.md`。

## 5. MVP 范围
- 创建任务：`POST /api/docgen/jobs`
- 任务查询：`GET /api/docgen/jobs/{jobId}`
- 聊天澄清：`POST /api/docgen/jobs/{jobId}/chat`
- 生成文档：`POST /api/docgen/jobs/{jobId}/generate`
- 导出文档：`GET /api/docgen/jobs/{jobId}/export`

## 6. 验收口径
- 任何任务必须可追踪到 `jobId + status + chatHistory`。
- 生成前必须检查 `unconfirmedItems` 是否为空。
- 导出文件名格式：`PRD-{jobId}-revised.md`。
