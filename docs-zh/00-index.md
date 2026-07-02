# 文档索引

> 📦 GitHub: [https://github.com/geekchow/micro-service-auth](https://github.com/geekchow/micro-service-auth)

讲解并逐步演示微服务系统中的认证与授权（authn/authz），配套一个示例 mobile-banking PoC。

## 一段话讲清整体故事

客户端通过 `Keycloak`（IdP）登录并拿到一个 JWT。它经由 `Kong`（网关 / PEP）调用银行 API。`Kong` 先用 `Keycloak` 对令牌做内省（introspection），再询问 `OPA`（PDP）该调用方是否被允许执行此操作。若允许，`Kong` 将请求转发给 `banking-api-service`（资源服务器），后者在返回数据前会独立地再次校验该 JWT。`alice` 只能读取自己的账户；`ops-admin` 可以读取任意账户。

## 阅读地图

### 第一部分 — 基础

- [01 — 概念](01-concepts.md) — 认证/授权、IdP、PEP、PDP、JWT（从这里开始）
- [02 — 本项目架构](02-this-project-architecture.md) — 概念如何映射到组件
- [03 — 请求流程](03-request-flows.md) — 端到端的完整故事
- [04 — 本地演示指南](04-local-demo-guide.md) — 跑起来，亲眼看它工作

### 第二部分 — 组件深入

- [05 — 组件巡览](05-component-tour.md) — 一段话概括全部五个组件
- [06 — Keycloak / IdP](06-keycloak-idp.md) — 签发令牌的身份提供方
- [07 — Kong](07-kong.md) — 网关 / PEP 及其 OPA 插件
- [08 — OPA](08-opa.md) — 策略决策点（PDP）及其 Rego 策略
- [09 — banking-api-service](09-banking-api-service.md) — 会再次校验的资源服务器
- [10 — identity-bootstrap-service](10-identity-bootstrap-service.md) — 演示用户的初始化

### 第三部分 — 令牌机制

- [11 — JWT 签名、校验与内省](11-jwt-signature-validation.md) — 签名、校验、内省
- [12 — JWKS 深入解析](12-jwks.md) — JWK/JWKS 以及如何按 `kid` 选择密钥
- [13 — 访问令牌与刷新令牌的生命周期](13-token-lifecycle.md) — 访问/刷新令牌与续期

### 第四部分 — 参考

- [14 — 请求与响应细节](14-request-response-reference.md) — 线级（wire-level）的请求头、请求体与声明

## 从哪里开始

- 初次接触该主题 → [01 — 概念](01-concepts.md)
- 想快速看懂系统 → [02 — 架构](02-this-project-architecture.md) + [03 — 请求流程](03-request-flows.md) + [04 — 演示指南](04-local-demo-guide.md)
- 需要线级的报文细节 → [14 — 请求与响应细节](14-request-response-reference.md)
