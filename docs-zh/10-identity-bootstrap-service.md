# 10 — identity-bootstrap-service（演示初始化）

一个把演示用户「种」进 `Keycloak`、从而让 PoC 可运行的小型内部服务。它不是真实的客户接入（onboarding）。

## 它做什么

- 在 `Keycloak` 中创建受演示管理的用户（`alice`、`ops-admin`）
- 设置密码、`customer_id`、`account_ids` 以及 realm 角色（`customer` 或 `ops-admin`）
- 为它所拥有的每个用户打上 `demo_managed=true` 属性，以便在重复运行时能安全地对齐（reconcile）
- 仅在 Compose 网络内部运行 —— 它没有公开端口，也不在 `Kong` 之后

## 它如何与 Keycloak 通信

`KeycloakAdminConfiguration` 装配了一个 `RestTemplate`（连接/读取超时均为 5 秒），并产出一个由 `KeycloakAdminProvisioner` 支撑的 `KeycloakUserProvisioner` bean。

`KeycloakAdminProperties`（前缀 `keycloak.admin`）保存服务所需的五项坐标：

| 属性 | Compose 环境变量 | 默认值 |
|---|---|---|
| `serverUrl` | `KEYCLOAK_ADMIN_SERVER_URL` | `http://keycloak:8080` |
| `realm` | `KEYCLOAK_ADMIN_REALM` | `banking-poc` |
| `adminRealm` | `KEYCLOAK_ADMIN_ADMIN_REALM` | `master` |
| `clientId` | `KEYCLOAK_ADMIN_CLIENT_ID` | `admin-cli` |
| `username` / `password` | `KEYCLOAK_ADMIN_USERNAME` / `KEYCLOAK_ADMIN_PASSWORD` | `admin` / `admin` |

对每次初始化调用，`KeycloakAdminProvisioner` 会：

1. 从 `POST /realms/master/protocol/openid-connect/token`（Resource Owner Password 授权，`admin-cli` client）获取一个短期管理员令牌。
2. 通过 `GET /admin/realms/banking-poc/users?username=…&exact=true` 查找目标用户名。
3. 若用户不存在 —— 调用 `POST /admin/realms/banking-poc/users`，提交包含凭据与属性的完整用户表示。
4. 若用户已存在 —— 先核实 `demo_managed=true` 属性存在（拒绝触碰非自己创建的用户），再调用 `PUT /admin/realms/banking-poc/users/{id}` 更新属性、调用 `PUT /admin/realms/banking-poc/users/{id}/reset-password` 重置密码。
5. 通过 `POST /admin/realms/banking-poc/users/{id}/role-mappings/realm` 同步 realm 角色，并移除任何不再需要的、受演示管理的角色（`customer`、`ops-admin`）。

`DemoUserService` 位于 `KeycloakAdminProvisioner` 之前，把守角色白名单 —— 只接受 `customer` 与 `ops-admin`；任何其他值返回 `400 Bad Request`。

`DemoUserController` 暴露 `POST /demo/users`，并强制校验经 `X-Demo-Bootstrap-Secret` 头传入的共享密钥（值来自 `DEMO_BOOTSTRAP_SECRET`；在 Compose 中默认为 `demo-bootstrap-secret`）。缺失或错误的密钥会在请求到达 `DemoUserService` 之前返回 `401 Unauthorized`。

## 实例演练：初始化 `alice`

向 `POST /demo/users` 的请求：

```http
POST /demo/users HTTP/1.1
X-Demo-Bootstrap-Secret: demo-bootstrap-secret
Content-Type: application/json

{
  "username":   "alice",
  "password":   "alice",
  "role":       "customer",
  "customerId": "C-1001",
  "accountIds": ["A-1001", "A-1002"]
}
```

请求体直接映射到 `DemoUserRequest`（一个 Java record，每个字段都带 `@NotBlank` / `@NotEmpty` 校验）。

`KeycloakAdminProvisioner` 把以下属性写入 `Keycloak`：

- `demo_managed` → `true`
- `customer_id` → `C-1001`
- `account_ids` → `["A-1001", "A-1002"]`

它还分配 `customer` realm 角色。

成功时，`DemoUserController` 返回 `201 Created`，响应体为 `DemoUserCreatedResponse`：

```json
{ "username": "alice", "role": "customer", "status": "created" }
```

各流程的完整线级报文见 [14](14-request-response-reference.md)。

## 它的位置

- 在 [03](03-request-flows.md) 的流程 1 中被触发 —— 在任何登录尝试之前运行一次
- 产出那些「密码与自定义属性驱动了 [06](06-keycloak-idp.md) 所述令牌内容」的用户
- 这里设置的 `customer_id` 与 `account_ids` 属性，会作为声明出现在 `Keycloak` 签发给 `alice` 或 `ops-admin` 的每个 JWT 中

## 需要记住什么

`identity-bootstrap-service` 是 PoC 的便利工具，而非真实的接入服务。它使用 `Keycloak` Admin REST API，配合一个 master-realm 管理员令牌与一个共享 bootstrap 密钥 —— 二者都是有意设置的不安全默认值，仅适用于本地 Compose 运行。`demo_managed` 属性充当安全护栏，确保即便你把该服务指向一个共享的 `Keycloak` 实例，对齐路径也绝不会覆盖真实用户。

---

← Prev: [09 — banking-api-service](09-banking-api-service.md) · Next: [11 — JWT 签名、校验与内省](11-jwt-signature-validation.md) →
