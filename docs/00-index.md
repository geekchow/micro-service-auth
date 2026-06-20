# Docs Index

Explanation and walkthrough for the mobile-banking authn/authz PoC.

## The story in one paragraph

A client logs in via `Keycloak` (IdP) and gets a JWT. It calls the banking API through
`Kong` (gateway / PEP). Kong introspects the token with Keycloak, then asks `OPA` (PDP)
whether this caller may take this action. If allowed, Kong forwards the request to
`banking-api-service` (resource server), which independently re-validates the JWT before
returning data. `alice` can read only her own accounts; `ops-admin` can read any account.

## Reading map

### Part I — Foundations
- [01 — Concepts](01-concepts.md) — authn/authz, IdP, PEP, PDP, JWT (start here)
- [02 — This Project Architecture](02-this-project-architecture.md) — how the concepts map to components
- [03 — Request Flows](03-request-flows.md) — the end-to-end stories
- [04 — Local Demo Guide](04-local-demo-guide.md) — run it and watch it work

### Part II — Component Deep Dives
- [05 — Component Tour](05-component-tour.md) — one-paragraph map of all five components
- [06 — Keycloak / IdP](06-keycloak-idp.md) — the IdP that issues tokens
- [07 — Kong](07-kong.md) — the gateway / PEP and its OPA plugin
- [08 — OPA](08-opa.md) — the PDP and its Rego policy
- [09 — banking-api-service](09-banking-api-service.md) — the resource server that re-validates
- [10 — identity-bootstrap-service](10-identity-bootstrap-service.md) — demo user setup

### Part III — Token Mechanics
- [11 — JWT Signature, Validation & Introspection](11-jwt-signature-validation.md) — signature, validation, introspection
- [12 — JWKS Deep Dive](12-jwks.md) — JWK/JWKS and key selection by `kid`
- [13 — Access & Refresh Token Lifecycle](13-token-lifecycle.md) — access/refresh tokens and renewal

### Part IV — Reference
- [14 — Request & Response Details](14-request-response-reference.md) — wire-level headers, bodies, claims

## Where to start
- New to the topic → [01 — Concepts](01-concepts.md)
- Want the system fast → [02 — Architecture](02-this-project-architecture.md) + [03 — Request Flows](03-request-flows.md) + [04 — Demo Guide](04-local-demo-guide.md)
- Need wire-level payloads → [14 — Request & Response Details](14-request-response-reference.md)

## Design and planning docs
- [Docs reorganization — design](superpowers/specs/2026-06-18-docs-reorganization-design.md)
- [Docs reorganization — plan](superpowers/plans/2026-06-18-docs-reorganization.md)
- [Mobile-banking auth — design](superpowers/specs/2026-06-05-mobile-banking-auth-design.md)
- [Mobile-banking auth — plan](superpowers/plans/2026-06-05-mobile-banking-auth-poc.md)
