# 08 Request And Response Details

This file explains the actual request headers, request bodies, token claims, and response bodies flowing between components in this PoC.

The goal is to show the wire-level view of the system.

## Why This Doc Exists

The higher-level docs explain:

- what each component does
- how the workflow looks logically

This doc explains:

- what data is actually sent
- which headers are used
- what JSON or form body each component receives
- what response shape comes back

## End-To-End Data Flow Map

```mermaid
sequenceDiagram
    participant D as Demo Script
    participant I as identity-bootstrap-service
    participant K as Keycloak
    participant G as Kong
    participant O as OPA
    participant B as banking-api-service

    D->>I: POST /demo/users + JSON + bootstrap header
    I->>K: Admin token request
    I->>K: Create or update demo user
    D->>K: Token request
    K-->>D: JWT access token
    D->>G: API request + Authorization Bearer token
    G->>K: Introspection request
    K-->>G: active/inactive response
    G->>O: JSON policy input
    O-->>G: allow/deny decision
    G->>B: Forwarded API request
    B-->>G: JSON banking response
    G-->>D: Final HTTP response
```

## Flow 1: Demo Script -> identity-bootstrap-service

The demo script creates demo users by calling:

- `POST http://identity-bootstrap-service:8080/demo/users`

### Headers

```http
X-Demo-Bootstrap-Secret: demo-bootstrap-secret
Content-Type: application/json
```

### Request Body Example For `alice`

```json
{
  "username": "alice",
  "password": "Password123!",
  "role": "customer",
  "customerId": "C-1001",
  "accountIds": ["A-1001"]
}
```

### Request Body Example For `ops-admin`

```json
{
  "username": "ops-admin",
  "password": "Password123!",
  "role": "ops-admin",
  "customerId": "C-9999",
  "accountIds": ["A-1001", "A-2001"]
}
```

### Response

On success, `identity-bootstrap-service` returns HTTP `201` with a body like:

```json
{
  "username": "alice",
  "role": "customer",
  "status": "created"
}
```

If the shared bootstrap header is missing, the service returns `401`.

If the role is not one of the allowed demo roles, the service returns `400`.

If the username already exists in Keycloak but is not marked as demo-managed, the service returns `409`.

## Flow 2: identity-bootstrap-service -> Keycloak Token Endpoint

Before the bootstrap service can create or update users, it authenticates to Keycloak as an admin client.

### Request

Method:

- `POST`

URL:

- `http://keycloak:8080/realms/master/protocol/openid-connect/token`

Headers:

```http
Content-Type: application/x-www-form-urlencoded
```

Form body:

```text
grant_type=password
client_id=admin-cli
username=admin
password=admin
```

Curl example:

```bash
curl -sS -X POST "http://keycloak:8080/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "client_id=admin-cli" \
  --data-urlencode "username=admin" \
  --data-urlencode "password=admin"
```

### Response

Keycloak returns JSON that contains at least:

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJrcTd4OGlTZDl3TGwtOUpSRkZ0MzE0NFZ2bnF3eFJMcWFwMjNxUEQ1bjRRIn0.eyJleHAiOjE3ODE0NTA3MDEsImlhdCI6MTc4MTQ1MDY0MSwianRpIjoiMWE2ZjNmODUtMzI1Zi00ZDg4LTgyODYtNWQyYTZkYmM2Yzc3IiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjgwODAvcmVhbG1zL21hc3RlciIsInR5cCI6IkJlYXJlciIsImF6cCI6ImFkbWluLWNsaSIsInNpZCI6IjJmOTE0ZjBlLTc2OTAtNDM1YS1iZjk1LWQ4ODcyMTNmY2E4ZSIsInNjb3BlIjoicHJvZmlsZSBlbWFpbCJ9.OFduJ4Zziz_lrs0CWlkksCZlnJmr6vbU31Z0ECuR8KvBgt6ALJ5w4zH50Gm1VwH-qhmaq-ZltuZtGiUUeL1vQJfKhSk69hSEqwQyDXLOpEiBeTZZ6OnhG1qMgdapqnRrv5qNtSFQY216S8pba1geHeP6ngzk27Ar0G443tC80TaFag6r3-n-1WgeGGZJz-QegcfIefzTLbw5p9Z0QFoQp2YfohF3TCRHJVlnNk3_70cEtLE_W_dEuJWhHhgq4GLIZE-zWK5-_ARK601geeaj8grjxGGtwg371YCG6QzwNr8FK78d6C9mMzrmrc21HCCGhln-6XYIlWs0DkUMdIxidw",
  "expires_in": 60,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzUxMiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI4ZTBlZTZkNi05ZmI2LTRlYTUtOTgxZS1lNmZiM2Y1NDBlMGYifQ.eyJleHAiOjE3ODE0NTI0NDEsImlhdCI6MTc4MTQ1MDY0MSwianRpIjoiNTEzYzdjOWYtMTZkYi00YjMzLWFmM2ItN2I3YzRkMzc3ZjhmIiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjgwODAvcmVhbG1zL21hc3RlciIsImF1ZCI6Imh0dHA6Ly9rZXljbG9hazo4MDgwL3JlYWxtcy9tYXN0ZXIiLCJ0eXAiOiJSZWZyZXNoIiwiYXpwIjoiYWRtaW4tY2xpIiwic2lkIjoiMmY5MTRmMGUtNzY5MC00MzVhLWJmOTUtZDg4NzIxM2ZjYThlIiwic2NvcGUiOiJwcm9maWxlIGJhc2ljIHdlYi1vcmlnaW5zIGFjciBlbWFpbCByb2xlcyJ9.9hSn0WBzbh3v5flTCybuo26tpT5g9R4FwFjf0sLybAkCBB-2nT8eXbSwEACg3gSj8RQGJR7oHFxUQT_6g4pEHQ",
  "token_type": "Bearer",
  "not-before-policy": 0,
  "session_state": "2f914f0e-7690-435a-bf95-d887213fca8e",
  "scope": "profile email"
}
```

The bootstrap service extracts:

- `access_token`

and uses it in later admin API calls.

## Flow 3: identity-bootstrap-service -> Keycloak Admin User APIs

After the bootstrap service gets an admin token, it calls several Keycloak admin endpoints.

### Find User By Username

Request:

```http
GET /admin/realms/banking-poc/users?username=alice&exact=true
Authorization: Bearer <admin-token>
```

Curl example:

```bash
curl -sS "http://keycloak:8080/admin/realms/banking-poc/users?username=alice&exact=true" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJrcTd4OGlTZDl3TGwtOUpSRkZ0MzE0NFZ2bnF3eFJMcWFwMjNxUEQ1bjRRIn0.eyJleHAiOjE3ODE0NTEyMDcsImlhdCI6MTc4MTQ1MTE0NywianRpIjoiMTRkMjNhYjQtNjI4ZS00MjAxLWEwZTktNTYwYjEzNDYyNWVjIiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjgwODAvcmVhbG1zL21hc3RlciIsInR5cCI6IkJlYXJlciIsImF6cCI6ImFkbWluLWNsaSIsInNpZCI6ImNjZGM3MWU2LWNlMjQtNDljNC05YTYwLWQzNDkxMTgxMDhhNiIsInNjb3BlIjoicHJvZmlsZSBlbWFpbCJ9.AGJc5AxiYKjT-js-f8I1pFFsXoshYqhRiXAauDi_VdfSrQmpceUYMvHYjhX2v81dLh5xy1kcBo_tmbgHNryyh-st_-QQ68BsIyYtW_1GXpnbTWnX_eI9546Dcyjuy5hmosBuWgj7jLBOJhT54shiGRnRAA5RSTpsHWGNC9qSW8nnIlBtVIWbUZhIzeD2_za-CaAfCoICvLWFkjlC79oZjRL3eOpvn8k_px_djQMxWQdQqOEjKSERrm_d7AW9r_NjDl4RNMvrt7v-QudtwGO-sSuTSASRLshceS0a0Zj5yb1ryFUrTtT7u7_NSVNt_sYn_d22wFCb0n_gMbVBXasKaA"
```

Response when not found:

```json
[]
```

Response when found:

```json
[
  {
    "id": "134d2448-334d-4be5-8868-4d13085bf2cd",
    "username": "alice",
    "firstName": "alice",
    "lastName": "Demo",
    "email": "alice@example.local",
    "emailVerified": false,
    "attributes": {
      "customer_id": [
        "C-1001"
      ],
      "account_ids": [
        "A-1001"
      ]
    },
    "createdTimestamp": 1780652643152,
    "enabled": true,
    "totp": false,
    "disableableCredentialTypes": [],
    "requiredActions": [],
    "notBefore": 0,
    "access": {
      "manageGroupMembership": true,
      "view": true,
      "mapRoles": true,
      "impersonate": true,
      "manage": true
    }
  }
]
```

### Create User

Request:

```http
POST /admin/realms/banking-poc/users
Authorization: Bearer <admin-token>
Content-Type: application/json
```

Body example:

```json
{
  "username": "alice",
  "enabled": true,
  "email": "alice@example.local",
  "firstName": "alice",
  "lastName": "Demo",
  "attributes": {
    "demo_managed": ["true"],
    "customer_id": ["C-1001"],
    "account_ids": ["A-1001"]
  },
  "credentials": [
    {
      "type": "password",
      "value": "Password123!",
      "temporary": false
    }
  ]
}
```

Response:

- HTTP `201 Created`
- `Location` header pointing to the new user resource

### Update Existing Demo-Managed User

Request:

```http
PUT /admin/realms/banking-poc/users/<userId>
Authorization: Bearer <admin-token>
Content-Type: application/json
```

Body example:

```json
{
  "username": "alice",
  "enabled": true,
  "email": "alice@example.local",
  "firstName": "alice",
  "lastName": "Demo",
  "attributes": {
    "demo_managed": ["true"],
    "customer_id": ["C-1001"],
    "account_ids": ["A-1001"]
  }
}
```

### Reset Password

Request:

```http
PUT /admin/realms/banking-poc/users/<userId>/reset-password
Authorization: Bearer <admin-token>
Content-Type: application/json
```

Body:

```json
{
  "type": "password",
  "value": "Password123!",
  "temporary": false
}
```

### Read And Sync Realm Roles

Current role lookup:

```http
GET /admin/realms/banking-poc/users/<userId>/role-mappings/realm
Authorization: Bearer <admin-token>
```

Requested role lookup:

```http
GET /admin/realms/banking-poc/roles/customer
Authorization: Bearer <admin-token>
```

Remove stale demo-managed role:

```http
DELETE /admin/realms/banking-poc/users/<userId>/role-mappings/realm
Authorization: Bearer <admin-token>
Content-Type: application/json
```

Add desired role:

```http
POST /admin/realms/banking-poc/users/<userId>/role-mappings/realm
Authorization: Bearer <admin-token>
Content-Type: application/json
```

Body example:

```json
[
  {
    "id": "role-customer",
    "name": "customer",
    "composite": false,
    "clientRole": false,
    "containerId": "banking-poc"
  }
]
```

## Flow 4: Demo Script -> Keycloak Login Endpoint

The script gets user tokens directly from Keycloak.

### Request

Method:

- `POST`

URL:

- `http://localhost:9081/realms/banking-poc/protocol/openid-connect/token`

Headers:

```http
Content-Type: application/x-www-form-urlencoded
```

Form body example:

```text
grant_type=password
client_id=mobile-banking-app
username=alice
password=Password123!
```

### Response

Keycloak returns JSON like:

```json
{
  "access_token": "<jwt>",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "token_type": "Bearer",
  "scope": "email profile"
}
```

The script extracts:

- `.access_token`

## Flow 5: JWT Claims Produced By Keycloak

Important claims in this PoC token include:

```json
{
  "iss": "http://keycloak:8080/realms/banking-poc",
  "aud": "mobile-banking-app",
  "preferred_username": "alice",
  "realm_access": {
    "roles": ["customer"]
  },
  "customer_id": "C-1001",
  "account_ids": ["A-1001"]
}
```

Why these matter:

- `iss`: Spring Boot checks the issuer
- `aud`: Spring Boot checks that the token is meant for `mobile-banking-app`
- `realm_access.roles`: Kong and Spring use roles
- `customer_id`: Spring uses it for defense-in-depth checks
- `account_ids`: Kong sends it to OPA and Spring uses it too

## Flow 6: Client -> Kong

The client calls Kong through:

- `http://localhost:8000/api/accounts/...`

### Example Request

```http
GET /api/accounts/A-1001 HTTP/1.1
Host: localhost:8000
Authorization: Bearer <jwt>
```

If the token is missing, Kong returns:

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json; charset=utf-8

{"message":"missing bearer token"}
```

If the token is malformed, Kong returns:

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json; charset=utf-8

{"message":"invalid bearer token"}
```

## Flow 7: Kong -> Keycloak Introspection

Before Kong uses the token claims for OPA input, it introspects the token.

### Request

Method:

- `POST`

URL:

- `http://keycloak:8080/realms/banking-poc/protocol/openid-connect/token/introspect`

Headers:

```http
Authorization: Basic base64(kong-introspection:kong-introspection-secret)
Content-Type: application/x-www-form-urlencoded
```

Body:

```text
token=<jwt>
```

### Response For A Valid Token

Typical response:

```json
{
  "active": true,
  "client_id": "mobile-banking-app",
  "username": "alice",
  "token_type": "Bearer",
  "exp": 1781236914,
  "iat": 1781236614,
  "sub": "134d2448-334d-4be5-8868-4d13085bf2cd",
  "aud": "mobile-banking-app",
  "iss": "http://keycloak:8080/realms/banking-poc"
}
```

### Response For A Tampered Or Invalid Token

Typical response:

```json
{
  "active": false
}
```

Kong then returns:

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json; charset=utf-8

{"message":"inactive token"}
```

## Flow 8: Kong -> OPA

If introspection says the token is active, Kong decodes JWT claims locally and constructs a JSON body for OPA.

### Request

Method:

- `POST`

URL:

- `http://opa:8181/v1/data/banking_authz/allow`

Headers:

```http
Content-Type: application/json
```

Body example for `alice` accessing `A-1001`:

```json
{
  "input": {
    "method": "GET",
    "path": "/api/accounts/A-1001",
    "account_id": "A-1001",
    "customer_id": "C-1001",
    "account_ids": ["A-1001"],
    "role": "customer",
    "username": "alice"
  }
}
```

### Response From OPA When Allowed

```json
{
  "result": true
}
```

### Response From OPA When Denied

```json
{
  "result": false
}
```

If OPA returns `false`, Kong returns:

```http
HTTP/1.1 403 Forbidden
Content-Type: application/json; charset=utf-8

{"message":"forbidden"}
```

## Flow 9: Kong -> banking-api-service

If OPA allows the request, Kong forwards it to:

- `http://banking-api-service:8080`

### Forwarded Request Example

```http
GET /api/accounts/A-1001 HTTP/1.1
Authorization: Bearer <jwt>
```

The banking service receives the bearer token and Spring Security turns it into a validated `Jwt` principal.

Then the service reads claims such as:

- `realm_access.roles`
- `customer_id`
- `account_ids`

## Flow 10: banking-api-service Responses

### Account Details Success

Endpoint:

- `GET /api/accounts/A-1001`

Response:

```json
{
  "accountId": "A-1001",
  "customerId": "C-1001",
  "currency": "GBP"
}
```

### Transactions Success

Endpoint:

- `GET /api/accounts/A-1001/transactions`

Response:

```json
[
  {
    "accountId": "A-1001",
    "amount": -12.35
  }
]
```

### Unknown Account

For both controllers, if the account ID does not exist after authorization checks, the service returns:

```http
HTTP/1.1 404 Not Found
```

### Forbidden At Service Layer

If a direct/internal call reaches the banking service with a valid token but the claims do not allow access, the service returns:

```http
HTTP/1.1 403 Forbidden
```

### Invalid JWT At Service Layer

If a request reaches the banking service with a token that fails JWT validation, Spring Security returns:

```http
HTTP/1.1 401 Unauthorized
```

## Flow 11: Which Component Sends Which Important Header

### Demo Script -> identity-bootstrap-service

- `X-Demo-Bootstrap-Secret`
- `Content-Type: application/json`

### identity-bootstrap-service -> Keycloak Token Endpoint

- `Content-Type: application/x-www-form-urlencoded`

### identity-bootstrap-service -> Keycloak Admin APIs

- `Authorization: Bearer <admin-token>`
- `Content-Type: application/json` for write operations

### Client -> Kong

- `Authorization: Bearer <jwt>`

### Kong -> Keycloak Introspection

- `Authorization: Basic <base64(clientId:clientSecret)>`
- `Content-Type: application/x-www-form-urlencoded`

### Kong -> OPA

- `Content-Type: application/json`

### Kong -> banking-api-service

- forwarded `Authorization: Bearer <jwt>`

## Quick Comparison Table

| Sender                     | Receiver                   | Main purpose                  | Body style                 |
| -------------------------- | -------------------------- | ----------------------------- | -------------------------- |
| Demo script                | identity-bootstrap-service | Create demo user              | JSON                       |
| identity-bootstrap-service | Keycloak token endpoint    | Get admin token               | Form URL encoded           |
| identity-bootstrap-service | Keycloak admin API         | Create/update users and roles | JSON                       |
| Demo script                | Keycloak token endpoint    | Get user JWT                  | Form URL encoded           |
| Client                     | Kong                       | Call protected banking API    | Usually no body for GET    |
| Kong                       | Keycloak introspection     | Validate token activity       | Form URL encoded           |
| Kong                       | OPA                        | Ask policy decision           | JSON                       |
| Kong                       | banking-api-service        | Forward allowed request       | Forwarded original request |

## Why This Level Of Detail Matters

At the architecture level, it is easy to say:

- Keycloak authenticates
- Kong enforces
- OPA decides
- Spring Boot serves data

At the payload level, the system becomes clearer because you can see:

- which component sends form data
- which one sends JSON
- which claims are extracted from the token
- why a request becomes `200`, `401`, `403`, or `404`

That is the real wire-level story of this PoC.
