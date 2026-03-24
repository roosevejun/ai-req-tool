# OpenAPI 与权限联动说明

本文档说明当前后端中 Swagger/OpenAPI、登录鉴权、权限校验三者如何协同工作。

## 1. 总体规则

1. 默认情况下，`/api/**` 接口都需要登录（Bearer Token）。
2. 需要公开访问的接口，必须显式加 `@PublicApi`。
3. 需要细粒度权限的接口，使用 `@RequiredPermission(...)`。
4. OpenAPI 文档会自动反映：
   - 是否需要 Bearer 鉴权
   - `401/403` 响应
   - `x-permissions` 扩展字段

## 2. 关键注解

## `@PublicApi`

- 作用：标记公开接口，跳过登录拦截。
- 位置：可加在类或方法上。
- 示例：

```java
@PostMapping("/login")
@PublicApi
public ApiResponse<LoginResponse> login(...) { ... }
```

## `@RequiredPermission`

- 作用：声明接口所需权限。
- 支持两类值：
  - 角色：`ROLE:ADMIN`
  - 业务权限码：如 `PROJECT:CREATE`
- 示例：

```java
@PostMapping
@RequiredPermission("PROJECT:CREATE")
public ApiResponse<Object> createProject(...) { ... }
```

## 3. 运行时生效点

## `AuthInterceptor`

- 处理顺序：
  1. `@PublicApi` -> 直接放行
  2. 校验 `Authorization: Bearer ...`
  3. 解析 token，加载用户信息
  4. 校验 `@RequiredPermission`
- `ADMIN` 角色具备权限校验旁路（超级管理员能力）。

## 4. 文档生效点

## `OpenApiConfig`

- 自动为非 `@PublicApi` 接口添加 `bearerAuth`。
- 自动补充 `401 Unauthorized`、`403 Forbidden`。
- 读取 `@RequiredPermission` 并写入：
  - `x-permissions` 扩展字段
  - operation 描述中的权限提示

## 5. 新增接口时的检查清单

1. 该接口是否公开？
   - 是：加 `@PublicApi`
   - 否：不加 `@PublicApi`
2. 该接口是否需要特定权限？
   - 是：加 `@RequiredPermission(...)`
3. 不要在控制器中重复写手工角色判断（避免与注解机制重复）。
4. 在 Swagger UI 中确认：
   - 公开接口无锁
   - 非公开接口有锁
   - 权限信息可见

## 6. 当前公开接口（约定）

1. `POST /api/system/auth/login`
2. `GET /api/docgen/health`

以上接口通过 `@PublicApi` 显式声明。

