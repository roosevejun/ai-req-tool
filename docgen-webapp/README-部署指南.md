# DocGen WebApp 部署指南（开发 / 测试 / 生产）

本文档给出项目在三种环境中的推荐部署方式和配置差异。

## 1. 环境规划

1. 开发环境（DEV）
- 用途：本地开发、自测、联调
- 特点：可用本地数据库，可开启 Mock/调试日志

2. 测试环境（TEST）
- 用途：功能测试、回归测试、UAT
- 特点：接近生产配置，使用独立数据库和密钥

3. 生产环境（PROD）
- 用途：正式业务
- 特点：高可用、最小权限、严格密钥管理与监控告警

---

## 2. 后端部署

在 `g:\Agent\docgen-webapp\backend` 打包：

```powershell
mvn -DskipTests package
```

产物：

- `target/docgen-agent-backend-0.1.0.jar`

启动示例：

```powershell
java -jar target/docgen-agent-backend-0.1.0.jar
```

---

## 3. 前端部署

在 `g:\Agent\docgen-webapp\frontend` 构建：

```powershell
npm install
npm run build
```

产物目录：

- `frontend/dist`

可由 Nginx 或静态资源服务托管。

---

## 4. 数据库初始化与回滚

初始化顺序：

1. `backend/src/main/resources/db/init-system.sql`
2. `backend/src/main/resources/db/init-project-requirement.sql`
3. 对已有数据库执行增量升级：
   `backend/src/main/resources/db/upgrade-project-metadata.sql`

说明：
- 新部署的新库，执行两个初始化脚本即可。
- 已存在的老库，在保留原数据的前提下，需要补执行 `upgrade-project-metadata.sql`。
- 该升级脚本会补齐项目产品化相关字段：
  `project_background`
  `similar_products`
  `target_customer_groups`
  `commercial_value`
  `core_product_value`
- 后端启动时会自动扫描并执行 `db/upgrade-*.sql`，并把执行记录写入 `sys_schema_upgrade`。
- 如果升级脚本内容发生变化，系统会根据脚本校验值重新执行，因此升级 SQL 必须保持幂等。

回滚脚本：

- `backend/src/main/resources/db/rollback-project-requirement.sql`

说明：
- 回滚脚本仅回滚“项目与需求模块”对象；
- 系统管理表（`sys_*`）默认保留。

---

## 5. 配置建议（按环境）

## DEV

- 可以使用 `application-local.yml`
- 可使用本地数据库
- 可以较宽松日志级别（INFO/DEBUG）
- 默认开启数据库自动升级，可通过 `DB_AUTO_UPGRADE_ENABLED=false` 临时关闭

## TEST

- 使用专用数据库
- 使用环境变量注入 token/key
- 打开接口与错误日志，便于定位问题

## PROD

必须满足：

1. 不在配置文件中明文存储任何真实密钥
2. `agent.auth.token.secret` 使用高强度随机值
3. 使用 HTTPS 与反向代理
4. 限制数据库账号权限（最小权限）
5. 配置日志留存、审计、告警

---

## 6. 关键环境变量

常用变量：

1. `AGENT_ROUTER_TOKEN`
2. `OPENAI_API_KEY`
3. `AGENT_AUTH_TOKEN_SECRET`
4. `AGENT_AUTH_TOKEN_EXPIRE_MINUTES`

优先级（当前实现）：

- `AGENT_ROUTER_TOKEN` > `OPENAI_API_KEY`

---

## 7. OpenAPI 与权限

Swagger 地址（默认）：

- `http://<host>:8080/swagger-ui/index.html`

规则：

1. 默认接口需要 Bearer Token
2. `@PublicApi` 为公开接口
3. `@RequiredPermission` 自动写入 `x-permissions`

详见：

- `backend/OPENAPI-AUTH-GUIDE.md`

---

## 8. 发布检查清单

1. 后端 `mvn test` 通过
2. 前端 `npm run build` 成功
3. 数据库初始化脚本或升级脚本已执行
4. 管理员账号已改密（非默认）
5. 生产密钥已完成注入
6. Swagger 关键接口验证通过（登录、项目、需求、生成）

