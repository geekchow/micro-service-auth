# 10 — identity-bootstrap-service (Demo Setup)

A small internal service that seeds demo users into `Keycloak` so the PoC is runnable. It is not real customer onboarding.

> **Part II · Component Deep Dives** — Prereqs: [03](03-request-flows.md), [06](06-keycloak-idp.md)

## What it does

- Creates demo-managed users in `Keycloak` (`alice`, `ops-admin`)
- Sets passwords, `customer_id`, `account_ids`, and realm roles (`customer` or `ops-admin`)
- Tags every user it owns with a `demo_managed=true` attribute so it can safely reconcile on re-runs
- Runs only inside the Compose network — it has no public port and is not behind `Kong`

## How it talks to Keycloak

`KeycloakAdminConfiguration` wires up a `RestTemplate` (5-second connect/read timeouts) and produces a `KeycloakUserProvisioner` bean backed by `KeycloakAdminProvisioner`.

`KeycloakAdminProperties` (prefix `keycloak.admin`) holds the five coordinates the service needs:

| Property | Compose env var | Default |
|---|---|---|
| `serverUrl` | `KEYCLOAK_ADMIN_SERVER_URL` | `http://keycloak:8080` |
| `realm` | `KEYCLOAK_ADMIN_REALM` | `banking-poc` |
| `adminRealm` | `KEYCLOAK_ADMIN_ADMIN_REALM` | `master` |
| `clientId` | `KEYCLOAK_ADMIN_CLIENT_ID` | `admin-cli` |
| `username` / `password` | `KEYCLOAK_ADMIN_USERNAME` / `KEYCLOAK_ADMIN_PASSWORD` | `admin` / `admin` |

For each provisioning call, `KeycloakAdminProvisioner`:

1. Fetches a short-lived admin token from `POST /realms/master/protocol/openid-connect/token` (Resource Owner Password grant, `admin-cli` client).
2. Looks up the target username via `GET /admin/realms/banking-poc/users?username=…&exact=true`.
3. If the user does not exist — calls `POST /admin/realms/banking-poc/users` with the full user representation including credentials and attributes.
4. If the user already exists — verifies the `demo_managed=true` attribute is present (refuses to touch users it did not create), then calls `PUT /admin/realms/banking-poc/users/{id}` to update attributes and `PUT /admin/realms/banking-poc/users/{id}/reset-password` to reset the password.
5. Syncs the realm role via `POST /admin/realms/banking-poc/users/{id}/role-mappings/realm`, removing any other demo-managed roles (`customer`, `ops-admin`) that are no longer wanted.

`DemoUserService` sits in front of `KeycloakAdminProvisioner` and guards the role whitelist — only `customer` and `ops-admin` are accepted; any other value returns `400 Bad Request`.

`DemoUserController` exposes `POST /demo/users` and enforces a shared secret passed in the `X-Demo-Bootstrap-Secret` header (value from `DEMO_BOOTSTRAP_SECRET`; defaults to `demo-bootstrap-secret` in Compose). A missing or wrong secret returns `401 Unauthorized` before the request reaches `DemoUserService`.

## Worked example: provisioning `alice`

Request to `POST /demo/users`:

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

The request body maps directly to `DemoUserRequest` (a Java record with `@NotBlank` / `@NotEmpty` validation on every field).

`KeycloakAdminProvisioner` writes the following attributes into `Keycloak`:

- `demo_managed` → `true`
- `customer_id` → `C-1001`
- `account_ids` → `["A-1001", "A-1002"]`

It also assigns the `customer` realm role.

On success, `DemoUserController` returns `201 Created` with a `DemoUserCreatedResponse` body:

```json
{ "username": "alice", "role": "customer", "status": "created" }
```

See [14](14-request-response-reference.md) for full wire payloads across all flows.

## Where it fits

- Triggered in Flow 1 of [03](03-request-flows.md) — run once before any login attempt
- Produces the users whose passwords and custom attributes drive the token content described in [06](06-keycloak-idp.md)
- The `customer_id` and `account_ids` attributes set here appear as claims in every JWT that `Keycloak` issues to `alice` or `ops-admin`

## What to remember

`identity-bootstrap-service` is a PoC convenience, not a real onboarding service. It uses the `Keycloak` Admin REST API with a master-realm admin token and a shared bootstrap secret — both are intentionally insecure defaults suitable only for local Compose runs. The `demo_managed` attribute acts as a safety guard so the reconcile path never overwrites real users if you point the service at a shared `Keycloak` instance.

---

← Prev: [09 — banking-api-service](09-banking-api-service.md) · Next: [11 — JWT Signature, Validation & Introspection](11-jwt-signature-validation.md) →
