## DocGen Agent 试用 Web 应用

功能流程：
1. 前端输入业务需求描述（自由文本）
2. 后端根据 `01-PRD-*.md` 模板先向用户索取澄清问题（JSON结构返回给前端）
3. 用户在页面回答澄清问题
4. 后端结合答案生成“需求文档（PRD）”的 Markdown（严格按模板结构输出）

注意：
- 这是“可运行骨架”，LLM 接入默认按 OpenAI Chat Completions 兼容实现。
- 若未配置 `OPENAI_API_KEY`，系统会启用 MockAgent 以便你先跑通交互流程。

目录结构：
- `backend`：Spring Boot
- `frontend`：Vue 3 + Vite

### 运行方式（本地）
1. 启动后端
   - 进入：`g:\Agent\docgen-webapp\backend`
   - 可选：设置环境变量 `OPENAI_API_KEY`（有key则调用LLM；无key则Mock）
   - 运行：`mvn spring-boot:run`
   - 端口：`http://localhost:8080`

2. 启动前端
   - 进入：`g:\Agent\docgen-webapp\frontend`
   - 运行：`npm install` 然后 `npm run dev`
   - 访问：`http://localhost:5173`

### 模板文件
- 后端会在 `agent.template.templateDir=../..`（默认指向 `g:\Agent`）下匹配 `01-PRD-*.md`，自动读取作为生成依据。

### WSL 运行时（Windows 后端 + WSL 前端）
如果你在 WSL 里运行前端，会出现 `ECONNREFUSED 127.0.0.1:8080`。
原因：WSL 的 `localhost` 不是 Windows 的 `localhost`。

解决方法：
1. 在 WSL 里找到 Windows 主机可达 IP（通常是默认网关）：
   - `ip route | grep default | awk '{print $3}'`
2. 用该 IP 测试后端是否可达：
   - `curl http://<windows-host-ip>:8080/api/docgen/health`
3. 启动前端时设置代理目标：
   - `export VITE_BACKEND_URL="http://<windows-host-ip>:8080"`
4. 再执行：`npm run dev`

