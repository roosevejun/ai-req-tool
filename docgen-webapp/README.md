# DocGen WebApp锛圓I 闇€姹傜鐞嗗伐鍏凤級

杩欐槸涓€涓潰鍚戦渶姹傜鐞嗗満鏅殑 AI 宸ュ叿锛屾敮鎸侊細

1. 閫氳繃瀵硅瘽鏀堕泦/婢勬竻闇€姹傘€?2. 鐢熸垚涓庤凯浠?PRD Markdown 鏂囨。銆?3. 椤圭洰绠＄悊銆侀渶姹傜鐞嗐€佺増鏈鐞嗐€?4. 鐢ㄦ埛鐧诲綍銆佺敤鎴?瑙掕壊/鏉冮檺绠＄悊锛圧BAC锛夈€?5. Swagger/OpenAPI 涓庢潈闄愯仈鍔ㄥ睍绀恒€?
---

## 鐩綍缁撴瀯

- `backend`锛歋pring Boot + MyBatis + PostgreSQL + Druid
- `frontend`锛歏ue 3 + Vite

---

## 鏍稿績鑳藉姏

### 1) AI 闇€姹傚伐浣滃彴

- 鍒涘缓浼氳瘽骞惰繘鍏ユ緞娓呴樁娈?- 閫愭瀵硅瘽琛ラ綈鏈‘璁ら」
- 鐢熸垚 PRD 骞朵繚瀛樼増鏈?- 瀵煎嚭鎸囧畾 PRD Markdown

### 2) 绯荤粺绠＄悊锛圧BAC锛?
- 鐢ㄦ埛绠＄悊锛氬垱寤恒€佹洿鏂般€侀噸缃瘑鐮併€佺粦瀹氳鑹?- 瑙掕壊绠＄悊锛氬垱寤恒€佹洿鏂般€佺粦瀹氭潈闄?- 鏉冮檺绠＄悊锛氬垱寤恒€佹洿鏂般€佸垪琛?
### 3) 椤圭洰涓庨渶姹傜鐞?
- 椤圭洰锛氬垪琛ㄣ€佽鎯呫€佸垱寤恒€佹洿鏂?- 闇€姹傦細鍒涘缓銆佺紪杈戙€佺増鏈煡鐪嬨€佺増鏈鍑?
### 4) OpenAPI 鏉冮檺鑱斿姩

- 榛樿鎺ュ彛瑕佹眰 Bearer Token
- `@PublicApi` 鎺ュ彛鑷姩鏍囪涓哄叕寮€
- `@RequiredPermission` 鑷姩鍐欏叆 `x-permissions`
- 鑷姩琛ュ厖 `401/403` 鍝嶅簲璇存槑

璇︾粏璇存槑瑙侊細
- `backend/OPENAPI-AUTH-GUIDE.md`

---

## 鐜瑕佹眰

- JDK 17
- Maven 3.9+
- Node.js 18+
- PostgreSQL 14+锛堟帹鑽愶級

---

## 鏁版嵁搴撳垵濮嬪寲

榛樿鏁版嵁搴撳悕寤鸿锛歚ai-req-tool`

鍦?PostgreSQL 涓緷娆℃墽琛岋細

1. `backend/src/main/resources/db/init-system.sql`
2. `backend/src/main/resources/db/init-project-requirement.sql`

鍒濆鍖栧悗浼氱敓鎴愰粯璁ょ鐞嗗憳璐﹀彿锛?
- 鐢ㄦ埛鍚嶏細`admin`
- 瀵嗙爜锛歚Admin@123`锛堝綋鍓嶈剼鏈负 `{noop}` 鏄庢枃鏂瑰紡锛屼粎鐢ㄤ簬寮€鍙戠幆澧冿級

---

## 鍚庣鍚姩

鍦?`g:\Agent\docgen-webapp\backend` 涓嬫墽琛岋細

```powershell
mvn spring-boot:run
```

榛樿鍦板潃锛?
- `http://localhost:8080`

Swagger UI锛?
- `http://localhost:8080/swagger-ui/index.html`

---

## 鍓嶇鍚姩

鍦?`g:\Agent\docgen-webapp\frontend` 涓嬫墽琛岋細

```powershell
npm install
npm run dev
```

榛樿鍦板潃锛?
- `http://localhost:5173`

---

## LLM 閰嶇疆

`application.yml` 褰撳墠璇诲彇浼樺厛绾э細

1. `OPENAI_API_KEY`

鍗筹細

```yaml
agent:
  llm:
    openai:
      apiKey: ${OPENAI_API_KEY:}
```

### 鎺ㄨ崘鏂瑰紡锛氱幆澧冨彉閲?
PowerShell锛?
```powershell
$env:OPENAI_API_KEY="your-key"
```

鎴栵細

```powershell
# optional: set another key if needed
# $env:OPENAI_API_KEY="another-key"
```

### 鏈湴閰嶇疆鏂囦欢锛堥伩鍏嶆彁浜ゅ瘑閽ワ級

澶嶅埗锛?
- `backend/src/main/resources/application-local.yml.example`
- 鍒?`backend/src/main/resources/application-local.yml`

骞跺～鍏ユ湰鍦?key銆傝鍕挎彁浜ょ湡瀹炲瘑閽ュ埌浠撳簱銆?
---

## WSL 鑱旇皟璇存槑锛圵indows 鍚庣 + WSL 鍓嶇锛?
濡傛灉鍓嶇鍦?WSL 閲岃繍琛岋紝璁块棶 `127.0.0.1:8080` 鍙兘澶辫触锛坄ECONNREFUSED`锛夈€?
鍙寜浠ヤ笅姝ラ澶勭悊锛?
1. 鍦?WSL 涓幏鍙?Windows 涓绘満 IP锛?
```bash
ip route | grep default | awk '{print $3}'
```

2. 娴嬭瘯鍚庣杩為€氭€э細

```bash
curl http://<windows-host-ip>:8080/api/docgen/health
```

3. 鍓嶇璁剧疆浠ｇ悊鐩爣锛?
```bash
export VITE_BACKEND_URL="http://<windows-host-ip>:8080"
npm run dev
```

---

## 娴嬭瘯

鍚庣娴嬭瘯锛?
```powershell
cd g:\Agent\docgen-webapp\backend
mvn test
```

褰撳墠宸插寘鍚?OpenAPI 鏉冮檺鑱斿姩鐨勫崟鍏冩祴璇曪紙`OpenApiConfigTest`锛夈€?
---

## 瀹夊叏娉ㄦ剰浜嬮」

1. 涓嶈鍦?`application.yml`銆乣application-local.yml`銆丷EADME 涓啓鍏ョ湡瀹炲瘑閽ャ€?2. 榛樿绠＄悊鍛樺瘑鐮佷粎鐢ㄤ簬寮€鍙戠幆澧冿紝閮ㄧ讲鍓嶈淇敼骞跺惎鐢ㄥ畨鍏ㄥ姞瀵嗙瓥鐣ャ€?3. 鐢熶骇鐜璇锋浛鎹?`agent.auth.token.secret`锛屼笉瑕佷娇鐢ㄩ粯璁ゅ€笺€?
---

## 鏇村鏂囨。

1. 閮ㄧ讲鎸囧崡锛歚README-閮ㄧ讲鎸囧崡.md`
2. OpenAPI 涓庢潈闄愯仈鍔細`backend/OPENAPI-AUTH-GUIDE.md`
3. 鍓嶅悗绔仈璋冮獙鏀舵竻鍗曪細`docs/鍓嶅悗绔仈璋冮獙鏀舵竻鍗?md`
4. 椤圭洰/闇€姹傛ā鍧楀洖婊氳剼鏈細`backend/src/main/resources/db/rollback-project-requirement.sql`

---

## P0 E2E Check Script

Run the end-to-end verification script (login -> project -> requirement -> AI workbench -> generate -> version export):

```powershell
cd g:\Agent\docgen-webapp
powershell -ExecutionPolicy Bypass -File .\scripts\e2e-p0-check.ps1
```

Optional parameters:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\e2e-p0-check.ps1 `
  -BackendBaseUrl "http://localhost:8080" `
  -Username "admin" `
  -Password "Admin@123" `
  -MaxChatRounds 6
```

---

## Docker 鎵撳寘涓庡惎鍔?
### 1) 鍑嗗鐜鍙橀噺

```powershell
cd g:\Agent\docgen-webapp
copy .env.docker.example .env
```

按需编辑 `.env`（尤其是 `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD`、`OPENAI_API_KEY`）。
如需切换模型/网关，可在 `.env` 配置：
- `OPENAI_BASE_URL`（例如 `https://aigc.x-see.cn/v1`）
- `OPENAI_MODEL`（例如 `gpt-3.5-turbo`）
- `OPENAI_TIMEOUT_MS`

### 2) 鏋勫缓骞跺惎鍔?
```powershell
docker compose up -d --build
```

鍚姩鍚庤闂細

- 鍓嶇: `http://localhost:5173`
- 鍚庣: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui/index.html`

榛樿绠＄悊鍛橈紙鍒濆鍖?SQL锛夛細

- 鐢ㄦ埛鍚? `admin`
- 瀵嗙爜: `Admin@123`

璇存槑锛氬綋鍓?`docker-compose.yml` 宸叉敼涓轰娇鐢ㄥ閮?PostgreSQL锛屼笉鍐嶅唴缃?`postgres` 瀹瑰櫒銆傝鍏堝湪澶栭儴鏁版嵁搴撴墽琛屽垵濮嬪寲鑴氭湰锛?
- `backend/src/main/resources/db/init-system.sql`
- `backend/src/main/resources/db/init-project-requirement.sql`

### 3) 鍋滄涓庢竻鐞?
```powershell
docker compose down
```

濡傞渶杩炲悓鏁版嵁搴撳嵎娓呯悊锛?
```powershell
docker compose down -v
```



---

## Separate Docker Compose (Backend / Frontend)

You can now build and run backend and frontend independently.

### Backend only

```powershell
cd g:\Agent\docgen-webapp\backend
copy ..\.env.docker.example .env
# edit .env for DB_* / OPENAI_* / AUTH_TOKEN_SECRET

docker compose --env-file .env up -d --build
```

### Frontend only

```powershell
cd g:\Agent\docgen-webapp\frontend
# optional: if backend is not on localhost:8080, set BACKEND_PROXY_PASS
$env:BACKEND_PROXY_PASS="http://host.docker.internal:8080"

docker compose up -d --build
```

Default frontend proxy target in independent mode:
- `http://host.docker.internal:8080`

This lets teams compile/deploy frontend and backend separately.
