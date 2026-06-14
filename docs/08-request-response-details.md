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

Before talking about where the claims come from, it helps to answer three basic questions:

1. what are JWT claims?
2. why do we need JWT claims?
3. what problem do JWT claims solve?

## Flow 5A: What JWT Claims Are

JWT claims are named pieces of information stored inside the JWT payload.

You can think of a claim as:

- a key-value fact about the authenticated user or token

Examples of claims are:

- who the user is
- which system issued the token
- which application the token is meant for
- what roles the user has
- what business-specific attributes belong to the user

In JSON form, claims look like this:

```json
{
  "iss": "http://keycloak:8080/realms/banking-poc",
  "aud": "mobile-banking-app",
  "preferred_username": "alice",
  "customer_id": "C-1001",
  "account_ids": ["A-1001"]
}
```

Each field in that JSON object is a claim.

### Standard Claims vs Custom Claims

There are two common types of claims:

#### Standard claims

These are well-known claims defined by JWT/OIDC conventions.

Examples:

- `iss`: issuer
- `aud`: audience
- `exp`: expiry time
- `sub`: subject identifier
- `iat`: issued-at time

These solve common token-validation problems.

#### Custom claims

These are application-specific claims added for business use.

Examples in this PoC:

- `customer_id`
- `account_ids`

These solve application-specific authorization problems.

## Flow 5B: Why We Need JWT Claims

After a user logs in, downstream systems still need context.

For example, Kong, OPA, and `banking-api-service` need to know things like:

- who is calling
- whether the token came from the right issuer
- whether the token is meant for this application
- what role the user has
- which customer/account scope belongs to the user

JWT claims carry that context.

Without claims, every downstream component would have to repeatedly ask another system questions like:

- Who is this user?
- What roles do they have?
- Which customer do they belong to?
- Which accounts are they allowed to access?

Claims let the token carry the important answers.

## Flow 5C: What Problem JWT Claims Solve

JWT claims solve several real distributed-system problems.

### Problem 1: Identity Propagation

Once a user logs in, multiple downstream components need to know who the user is.

Claims solve this by carrying identity information inside the token.

Example:

- `preferred_username = alice`
- `sub = 134d2448-334d-4be5-8868-4d13085bf2cd`

### Problem 2: Token Validation Context

Services need to know whether the token is trustworthy.

Claims help with that too.

Examples:

- `iss` tells the service who issued the token
- `aud` tells the service who the token is meant for
- `exp` tells the service when the token expires

Without these claims, the token would be much harder to validate safely.

### Problem 3: Authorization Context Propagation

A valid identity is not enough.

The system also needs enough information to make authorization decisions.

Claims solve this by carrying authorization-related context.

Examples in this PoC:

- `realm_access.roles = ["customer"]`
- `customer_id = C-1001`
- `account_ids = ["A-1001"]`

That lets OPA and Spring Boot decide whether `alice` may access `A-1001`.

### Problem 4: Reducing Repeated Lookups

If every service had to call Keycloak or another database for every request just to learn the caller's identity and scope, the system would become:

- slower
- more tightly coupled
- more complex

Claims reduce that repeated lookup cost by carrying the needed context with the request.

## Flow 5D: What JWT Claims Do Not Solve

Claims are useful, but they are not magic.

Claims do not solve:

- policy logic by themselves
- token trust by themselves
- stale business data by themselves

Important examples:

- a decoded claim is not automatically trustworthy
- a valid token can still be unauthorized for a specific action
- if the source of truth changes, old tokens can still contain older claim values until they expire

That is why this PoC still uses:

- Keycloak introspection in Kong
- JWT validation in Spring Boot
- OPA policy evaluation
- service-side authorization checks

## Flow 5E: Which Claims This PoC Uses And Why

### `iss`

- meaning: who issued the token
- used by: Spring Boot
- purpose: reject tokens not issued by this Keycloak realm

### `aud`

- meaning: which client/application the token is meant for
- used by: Spring Boot
- purpose: reject tokens not meant for `mobile-banking-app`

### `preferred_username`

- meaning: human-readable username
- used by: Kong and diagnostics
- purpose: helpful identity context for logging and policy input

### `realm_access.roles`

- meaning: realm roles assigned to the user
- used by: Kong, OPA, Spring Boot
- purpose: distinguish `customer` from `ops-admin`

### `customer_id`

- meaning: business identifier for the banking customer
- used by: OPA and Spring Boot
- purpose: connect the authenticated identity to banking ownership context

### `account_ids`

- meaning: which accounts the token claims this user can access
- used by: OPA and Spring Boot
- purpose: account-level authorization

## Flow 5F: JWT Claims In This PoC At A Glance

```mermaid
flowchart LR
    L[User logs in] --> K[Keycloak issues JWT]
    K --> C1[Identity claims
preferred_username sub]
    K --> C2[Validation claims
iss aud exp]
    K --> C3[Authorization claims
roles customer_id account_ids]
    C1 --> G[Kong]
    C2 --> S[Spring Security]
    C3 --> O[OPA and service guard]
```

This is the point where many people ask an important question:

- where do these JWT claims actually come from?

The short answer is:

1. `identity-bootstrap-service` writes user attributes into Keycloak
2. Keycloak protocol mappers copy those attributes into the token
3. Keycloak signs the JWT
4. Kong and Spring later read those claims from the JWT payload

So the claims are not invented by Kong or Spring Boot.
They originate in Keycloak.

## Flow 5G: Where The Claims Come From

### Step 1: Demo User Attributes Are Written Into Keycloak

When `identity-bootstrap-service` creates or updates a user, it sends user attributes like this:

```json
{
  "attributes": {
    "demo_managed": ["true"],
    "customer_id": ["C-1001"],
    "account_ids": ["A-1001"]
  }
}
```

That data is produced by the Java code in `KeycloakAdminProvisioner`:

```java
private Map<String, List<String>> attributes(DemoUserRequest request) {
    return Map.of(
            DEMO_MANAGED_ATTRIBUTE, List.of(DEMO_MANAGED_VALUE),
            "customer_id", List.of(request.customerId()),
            "account_ids", request.accountIds());
}
```

So for `alice`, Keycloak stores:

- `customer_id = C-1001`
- `account_ids = [A-1001]`

For `ops-admin`, Keycloak stores:

- `customer_id = C-9999`
- `account_ids = [A-1001, A-2001]`

### Step 2: Realm Roles Are Stored In Keycloak

The bootstrap service also assigns a realm role such as:

- `customer`
- `ops-admin`

Keycloak automatically places realm roles into the token under:

- `realm_access.roles`

### Step 3: Protocol Mappers Copy Attributes Into The Token

In `infra/keycloak/realm-export.json`, the client `mobile-banking-app` has protocol mappers.

These mappers are the bridge between:

- stored user attributes in Keycloak
- claims visible in the JWT

For example:

#### `customer_id` mapper

```json
{
  "name": "customer_id",
  "protocolMapper": "oidc-usermodel-attribute-mapper",
  "config": {
    "access.token.claim": "true",
    "claim.name": "customer_id",
    "user.attribute": "customer_id"
  }
}
```

Meaning:

- read the Keycloak user attribute named `customer_id`
- place it into the access token claim named `customer_id`

#### `account_ids` mapper

```json
{
  "name": "account_ids",
  "protocolMapper": "oidc-usermodel-attribute-mapper",
  "config": {
    "access.token.claim": "true",
    "claim.name": "account_ids",
    "user.attribute": "account_ids",
    "multivalued": "true"
  }
}
```

Meaning:

- read the Keycloak user attribute named `account_ids`
- place it into the access token claim named `account_ids`
- keep it as an array because it is multivalued

#### audience mapper

```json
{
  "name": "mobile-banking-app-audience",
  "protocolMapper": "oidc-audience-mapper",
  "config": {
    "access.token.claim": "true",
    "included.client.audience": "mobile-banking-app"
  }
}
```

Meaning:

- add `mobile-banking-app` into the token audience claim

That is why Spring Boot later expects:

- `aud = mobile-banking-app`

## Flow 5H: How The JWT Is Structured

A JWT has three parts:

```text
header.payload.signature
```

Each part is Base64URL-encoded.

### JWT Structure Diagram

```mermaid
flowchart LR
    H[Header] --> D1[Base64URL]
    P[Payload Claims] --> D2[Base64URL]
    S[Signature] --> D3[Base64URL]
    D1 --> J[header.payload.signature]
    D2 --> J
    D3 --> J
```

### Header

The header describes the token metadata, usually things like:

- signing algorithm
- key ID
- token type

Example:

```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "..."
}
```

### Payload

The payload contains the claims.

This is the part Kong decodes in the plugin when it wants to read values like:

- `preferred_username`
- `customer_id`
- `account_ids`
- `realm_access.roles`

Example payload shape in this PoC:

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

### Signature

The signature proves the token was signed by the issuer.

Important security idea:

- decoding the payload is easy
- trusting the payload is not safe until the token is validated

That is why this PoC does not rely on raw decoding alone.

## Flow 5I: Why Kong Can Decode Claims But Still Must Validate

The Kong plugin contains code that decodes the JWT payload segment:

```lua
local function decode_claims(token)
  local payload_segment = token:match("^[^.]+%.([^.]+)%.([^.]+)$")
  ...
  return cjson.decode(decoded)
end
```

This lets Kong read claims from the payload.

But payload decoding alone is not enough, because anyone can create a fake string that looks like a JWT.

So the plugin first calls Keycloak introspection:

1. Kong receives bearer token
2. Kong calls Keycloak introspection endpoint
3. Keycloak responds with `active: true` or `active: false`
4. only then does Kong use the decoded payload to build OPA input

That is the difference between:

- reading claims
- trusting claims

## Flow 5J: How Spring Boot Retrieves The Same Claims

Spring Boot does not parse claims by doing manual Base64 decoding.

Instead:

1. Spring Security validates the JWT
2. it creates a `Jwt` principal object
3. controller and guard code read claims from that validated `Jwt`

Examples from the banking service:

- `jwt.getClaimAsString("customer_id")`
- `jwt.getClaimAsStringList("account_ids")`
- reading `realm_access.roles`

So the same claim values are used in two places:

- Kong -> OPA path
- Spring service-side defense-in-depth path

## Flow 5K: End-To-End Claim Pipeline For `alice`

```mermaid
flowchart LR
    A[Demo request for alice] --> B[identity-bootstrap-service]
    B --> C[Keycloak user attributes:
customer_id=C-1001
account_ids=A-1001]
    C --> D[Keycloak protocol mappers]
    D --> E[JWT payload claims:
customer_id
account_ids
aud
realm_access.roles]
    E --> F[Kong decodes claims after introspection]
    E --> G[Spring Security validates and exposes Jwt claims]
    F --> H[OPA input]
    G --> I[service-side account guard]
```

### Concrete Example For `alice`

Input stored in Keycloak user profile:

```json
{
  "customer_id": ["C-1001"],
  "account_ids": ["A-1001"]
}
```

Role assigned in Keycloak:

```json
{
  "realm_access": {
    "roles": ["customer"]
  }
}
```

Audience added by mapper:

```json
{
  "aud": "mobile-banking-app"
}
```

Final useful payload seen by the rest of the system:

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
