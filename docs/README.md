# Docs Index

This directory contains the explanation and walkthrough documents for the mobile banking auth PoC.

## Recommended Reading Order

1. `01-concepts.md`
   - basic ideas: authentication, authorization, IdP, PEP, PDP, JWT

2. `02-this-project-architecture.md`
   - how those ideas map to this project

3. `03-request-flows.md`
   - the main end-to-end workflows

4. `04-component-deep-dives.md`
   - practical explanation of each major component

5. `05-local-demo-guide.md`
   - how to run and inspect the PoC locally

6. `06-idp-keycloak-deep-dive.md`
   - detailed explanation of IdP concepts and Keycloak in this repo

7. `07-kong-integration.md`
   - how Kong is configured and how it interoperates with the other components

8. `08-request-response-details.md`
   - low-level headers, bodies, claims, and responses between components

9. `09-opa-integration.md`
   - how OPA works from policy-engine principles to the actual Rego, tests, and runtime integration in this project
   - includes the banking API JWT validation config used in Flow 10

9. `09-jwt-signature-validation.md`
   - how JWT signatures, validation, introspection, and trust boundaries work with Keycloak
   - the deeper explanation behind the Flow 10 banking API validation settings

## Design And Planning Docs

- `superpowers/specs/2026-06-05-mobile-banking-auth-design.md`
- `superpowers/plans/2026-06-05-mobile-banking-auth-poc.md`

## Quick Pointers

- If you are new to the topic, start with `01-concepts.md`.
- If you want to understand the whole system quickly, read `02-this-project-architecture.md` and `03-request-flows.md` next.
- If you want wire-level payload details, jump to `08-request-response-details.md`.
