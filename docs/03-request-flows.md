# 03 Request Flows

This file explains the main workflows in the PoC.

## Flow 1: Demo User Setup

Purpose:

- create a repeatable demo user in Keycloak

Steps:

1. The demo script calls `identity-bootstrap-service`
2. The bootstrap service authenticates as Keycloak admin
3. The bootstrap service creates or updates a demo-managed user
4. The service assigns claims and roles such as `customer` or `ops-admin`

```mermaid
sequenceDiagram
    participant D as Demo Script
    participant B as identity-bootstrap-service
    participant K as Keycloak

    D->>B: POST /demo/users
    B->>K: Admin token request
    K-->>B: Admin access token
    B->>K: Create or reconcile demo user
    B->>K: Assign role and attributes
    K-->>B: User ready
    B-->>D: 201 created
```

## Flow 2: User Login

Purpose:

- exchange username and password for a JWT

Steps:

1. The client calls Keycloak token endpoint
2. Keycloak validates credentials
3. Keycloak returns a signed JWT

```mermaid
sequenceDiagram
    participant C as Client
    participant K as Keycloak

    C->>K: username + password
    K->>K: validate credentials
    K-->>C: JWT access token
```

## Flow 3: Allowed Account Access

Example:

- `alice` accesses `A-1001`

Steps:

1. Client sends request with JWT to Kong
2. Kong introspects the token with Keycloak
3. Kong decodes claims and sends policy input to OPA
4. OPA returns `allow`
5. Kong forwards to banking service
6. Banking service validates JWT again
7. Banking service checks account access again
8. Banking service returns the account data

```mermaid
sequenceDiagram
    participant C as Client
    participant G as Kong
    participant K as Keycloak
    participant O as OPA
    participant B as banking-api-service

    C->>G: GET /api/accounts/A-1001 + JWT
    G->>K: Introspect token
    K-->>G: active token
    G->>O: account_id + claims + role
    O-->>G: allow
    G->>B: Forward request
    B->>B: Validate JWT signature/issuer/audience
    B->>B: Validate account access
    B-->>G: 200 account data
    G-->>C: 200 account data
```

## Flow 4: Forbidden Account Access

Example:

- `alice` tries to access `A-2001`

Steps:

1. Kong verifies the token is active
2. Kong sends request data and claims to OPA
3. OPA returns `deny`
4. Kong stops the request and returns `403`

```mermaid
sequenceDiagram
    participant C as Client
    participant G as Kong
    participant K as Keycloak
    participant O as OPA

    C->>G: GET /api/accounts/A-2001 + JWT
    G->>K: Introspect token
    K-->>G: active token
    G->>O: account_id=A-2001 + alice claims
    O-->>G: deny
    G-->>C: 403 forbidden
```

## Flow 5: Missing Or Tampered Token

Missing token:

1. Client calls Kong without bearer token
2. Kong returns `401`

Tampered token:

1. Client sends an altered token
2. Kong introspection sees the token as inactive
3. Kong returns `401`

```mermaid
sequenceDiagram
    participant C as Client
    participant G as Kong
    participant K as Keycloak

    C->>G: API request without token or with tampered token
    alt missing token
        G-->>C: 401 unauthorized
    else tampered token
        G->>K: Introspect token
        K-->>G: inactive
        G-->>C: 401 unauthorized
    end
```

## Why The Flows Matter

These flows show the separation clearly:

- Keycloak authenticates
- Kong enforces edge access
- OPA decides policy
- Spring Boot provides business behavior and extra safety checks
