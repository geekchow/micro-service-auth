# Docs Reorganization — Design

**Date:** 2026-06-18
**Status:** Approved (design); pending implementation plan
**Scope:** Full rewrite pass over `docs/*.md` (excluding `docs/superpowers/`) to turn the
existing 13-file series into one organic, systematic, easy-to-follow learning sequence.

## Problem

The current docs are a strong body of content but have grown organically, producing
structural friction that hurts the step-by-step learning experience:

1. **Number collisions** — two files numbered `09` (`09-jwt-signature-validation.md`,
   `09-opa-integration.md`) and two numbered `10` (`10-banking-api-service-authn-authz.md`,
   `10-jwks-deep-dive.md`). File order is ambiguous.
2. **Orphan doc** — `10-banking-api-service-authn-authz.md` is not listed in the README
   reading order at all.
3. **Interleaved mental models** — per-component deep dives (Keycloak, Kong, OPA,
   banking-service) are mixed with cross-cutting mechanism docs (JWT signature, JWKS, token
   lifecycle) and one giant wire-level reference (`08`, 1423 lines), so the learning arc
   zig-zags between "layers."
4. **Content duplication** — introspection, JWKS, session/token lifecycle, and the JWT claim
   catalog are each explained in multiple docs with no single owner.

## Goals

- A single, progressive learning sequence a newcomer can read top to bottom.
- Clean, collision-free numbering grouped into named Parts.
- Each concept owned by exactly one doc; others cross-link.
- Consistent voice, terminology, examples, and per-doc navigation across all docs.
- No invented behavior: every factual claim verified against the repo.

## Non-Goals

- Changing how the PoC actually behaves (no code/config changes to the running system).
- Rewriting `docs/superpowers/` specs and plans.
- Adding new features to the PoC itself.

## Decisions (confirmed with user)

- **Scope:** Full rewrite pass — reorganize structure AND rewrite each doc for consistent
  tone, terminology, and section shape; merge/split content where it helps.
- **Spine:** Grouped into named Parts (Foundations → Components → Token Mechanics → Reference).
- **Demo guide** moves up into Part I (run it before the deep dives).
- **Wire-level reference** (`08`) moves to the very end as a Part IV lookup reference.
- **identity-bootstrap-service** gets its own short doc so all five components are symmetric.
- **Personas:** `alice` (customer) and `ops-admin` (any-account reader) used in every example.

## Target Structure

Flat `01..14` numbering; README carries the Part map. "From" = source file in the current
tree. Files renamed with `git mv` to preserve history.

### Part I — Foundations (build the mental model, then run it)

| New | Title | From |
|-----|-------|------|
| 01 | Concepts (authn/authz, IdP, PEP, PDP, JWT) | `01-concepts.md` |
| 02 | This Project Architecture | `02-this-project-architecture.md` |
| 03 | Request Flows | `03-request-flows.md` |
| 04 | Local Demo Guide | `05-local-demo-guide.md` |

### Part II — Component Deep Dives (one box at a time, request-path order)

| New | Title | From |
|-----|-------|------|
| 05 | Component Tour | `04-component-deep-dives.md` |
| 06 | Keycloak / IdP | `06-idp-keycloak-deep-dive.md` |
| 07 | Kong (gateway / PEP) | `07-kong-integration.md` |
| 08 | OPA (PDP) | `09-opa-integration.md` |
| 09 | banking-api-service (resource server) | `10-banking-api-service-authn-authz.md` |
| 10 | identity-bootstrap-service (demo setup) | new — extracted from Component Tour blurb + admin flows in current `08` |

### Part III — Token Mechanics (cross-cutting: how the token itself works)

| New | Title | From |
|-----|-------|------|
| 11 | JWT Signature, Validation & Introspection | `09-jwt-signature-validation.md` |
| 12 | JWKS Deep Dive | `10-jwks-deep-dive.md` |
| 13 | Access & Refresh Token Lifecycle | `11-access-token-refresh-token-lifecycle.md` |

### Part IV — Reference

| New | Title | From |
|-----|-------|------|
| 14 | Request & Response Details (wire-level) | `08-request-response-details.md` |

## Per-Doc Rewrite Template

Every doc gets the same skeleton so the series reads as one work:

1. **Title** — `# NN — Title` (em-dash, matches README).
2. **One-line "what this is."**
3. **Where you are** — breadcrumb: which Part, prereq docs, what this unlocks.
4. **Body sections** — scaled to the topic; deep dives keep their rich content.
5. **Worked example** — anchored to `alice` and `ops-admin`.
6. **Mental model / recap** — short "what to remember."
7. **Next** — link to the next doc.

### Shared conventions

- **Two running personas everywhere:** `alice` (customer, owns her accounts) and `ops-admin`
  (reads any account). No new ad-hoc names.
- **Terminology locked:** IdP = Keycloak, PEP = Kong, PDP = OPA, resource server =
  banking-api-service. Glossary lives in `01`; other docs link back rather than re-define.
- **Formatting:** backticks for all identifiers, claims, files, endpoints.
- **Diagrams:** Mermaid, with the same node labels for the same boxes across all diagrams.
- **Voice:** second person, present tense, short sentences (normalizing the existing style).

### What rewrite will / won't touch

- Will: section ordering, headings, transitions, intros/recaps, terminology, example
  consistency, dedup, cross-links.
- Won't: invent new technical claims or change PoC behavior. Real config/code is verified
  against the repo before rephrasing.

## Navigation & README

The README becomes the map:

1. Short intro — what the PoC is (2–3 sentences).
2. The one-paragraph story — the request journey
   (`alice` → Kong → Keycloak introspection → OPA → banking-service).
3. Reading map by Part — each doc as `NN — Title — one-line hook` with a "read this if…" pointer.
4. Three entry points — "New here → 01", "System fast → 02 + 03 + 04", "Wire-level → 14".
5. Keep existing pointers to `superpowers/specs` and `superpowers/plans`.

Per-doc navigation:

- Top of each doc: `Part II · Component Deep Dives` + `Prereqs: 01, 02`.
- Bottom of each doc: `← Prev: NN Title · Next: NN Title →`.

## Dedup & Content Consolidation

Ownership map — one doc owns each concept; others give a 1-line summary and link.

| Concept | Currently duplicated in | Owner | Others become |
|---------|------------------------|-------|---------------|
| Introspection mechanics | 06, 07, 09-jwt | **11** | 1-line summary + `see 11` |
| JWKS / key-by-`kid` | 09-jwt, 10-jwks | **12** | 11 keeps the JWKS-vs-introspection *decision*, defers mechanics to 12 |
| Access/refresh + session lifecycle | 06 (~250 lines), 09-jwt, 11-lifecycle | **13** | 06 keeps only Keycloak-specific session storage + "why it can answer `active:false`"; generic lifecycle → 13 |
| JWT claim catalog & wire payloads | 08, 06, 10-banking, 09-opa | **14** | component docs list only the claims they consume, link to 14 |

**Notable surgery:** the current Keycloak doc (`06`, 664 lines) carries a large amount of
generic token/session/introspection theory that belongs to Part III. That theory moves into
`11`/`13`; `06` is left focused on Keycloak as the IdP in this PoC. Everything else is
trimming + cross-linking.

**Guardrail:** before trimming anything that describes real behavior, verify against the repo
(`infra/`, `services/`, `docker-compose.yml`, the Rego policy, `SecurityConfig`, etc.) so
consolidation never drops a true detail or invents one.

## Acceptance Criteria

- 14 docs, numbered `01..14`, no collisions, no orphans.
- README reflects the Part map and lists every doc.
- Every doc follows the rewrite template (breadcrumb top, prev/next bottom, `alice`/`ops-admin`
  examples, recap).
- Each concept in the ownership map appears in full in exactly one doc; others link.
- `git mv` used for renames so history is preserved.
- No factual regressions: spot-checked claims match the repo.
