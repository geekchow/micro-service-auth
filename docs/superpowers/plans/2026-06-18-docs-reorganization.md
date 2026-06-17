# Docs Reorganization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize and fully rewrite the 13-file `docs/*.md` series into a 14-doc, four-part, collision-free learning sequence with consistent structure, navigation, personas, and single-owner concepts.

**Architecture:** Rename files first with `git mv` (two-phase to avoid collisions) so all final numbers/links are stable, then rewrite each doc to a shared template, redistribute duplicated theory to its owning doc, add the new identity-bootstrap doc, and finish with the README map and a verification pass.

**Tech Stack:** Markdown, Mermaid diagrams, git, shell (grep/find) for verification. No application code changes.

**Source spec:** `docs/superpowers/specs/2026-06-18-docs-reorganization-design.md`

---

## Conventions used by every doc task

When a task says "apply the standard template," it means:

**Header block** (immediately after the `# NN — Title` line and a one-line "what this is"):

```markdown
> **Part <ROMAN> · <Part Name>** — Prereqs: [01](01-concepts.md), [02](02-this-project-architecture.md)
```

(List the real prereq docs for that doc; Part I docs may have fewer prereqs.)

**Footer block** (last lines of the file):

```markdown
---

← Prev: [NN — Title](NN-file.md) · Next: [NN — Title](NN-file.md) →
```

(Doc 01 has no Prev; doc 14 has no Next.)

**Personas:** every worked example uses `alice` (customer who owns her own accounts) and/or `ops-admin` (reads any account). Remove any other ad-hoc example names.

**Terminology (locked):** IdP = Keycloak, PEP = Kong, PDP = OPA, resource server = banking-api-service. Define each once in `01`; elsewhere link to `01` instead of redefining.

**Formatting:** backticks for every identifier, claim name, file path, endpoint. Mermaid for diagrams, reusing identical node labels across docs (`Keycloak`, `Kong`, `OPA`, `banking-api-service`, `identity-bootstrap-service`, `Client`).

**Verification idiom** (used in many tasks): run from `docs/`.

```bash
# breadcrumb + nav present
grep -q '^> \*\*Part' NN-file.md && grep -q 'Next:\|Prev:' NN-file.md && echo "nav OK"
# no stray old example names (edit list per repo)
! grep -nE '\b(bob|carol|charlie|john)\b' NN-file.md && echo "personas OK"
# all relative .md links in this file resolve
for l in $(grep -oE '\]\(([0-9]{2}-[a-z-]+\.md)' NN-file.md | sed -E 's/\]\(//'); do test -f "$l" || echo "BROKEN: $l"; done; echo "links checked"
```

---

## Final file map (target → source)

| New file | Source file |
|----------|-------------|
| `01-concepts.md` | `01-concepts.md` (in place) |
| `02-this-project-architecture.md` | `02-this-project-architecture.md` (in place) |
| `03-request-flows.md` | `03-request-flows.md` (in place) |
| `04-local-demo-guide.md` | `05-local-demo-guide.md` |
| `05-component-tour.md` | `04-component-deep-dives.md` |
| `06-keycloak-idp.md` | `06-idp-keycloak-deep-dive.md` |
| `07-kong.md` | `07-kong-integration.md` |
| `08-opa.md` | `09-opa-integration.md` |
| `09-banking-api-service.md` | `10-banking-api-service-authn-authz.md` |
| `10-identity-bootstrap-service.md` | new |
| `11-jwt-signature-validation.md` | `09-jwt-signature-validation.md` |
| `12-jwks.md` | `10-jwks-deep-dive.md` |
| `13-token-lifecycle.md` | `11-access-token-refresh-token-lifecycle.md` |
| `14-request-response-reference.md` | `08-request-response-details.md` |

---

## Task 1: Rename files into the final layout (two-phase, no collisions)

**Files:** all of `docs/*.md` except `README.md`. The rename is a permutation with cycles (e.g. `04↔05`), so move every file to a temp name first, then to its final name.

- [ ] **Step 1: Phase one — move every source file to a temp name**

Run from `docs/`:

```bash
cd /Users/phil/ML/micro-service-auth/docs
git mv 05-local-demo-guide.md tmp-04.md
git mv 04-component-deep-dives.md tmp-05.md
git mv 06-idp-keycloak-deep-dive.md tmp-06.md
git mv 07-kong-integration.md tmp-07.md
git mv 09-opa-integration.md tmp-08.md
git mv 10-banking-api-service-authn-authz.md tmp-09.md
git mv 09-jwt-signature-validation.md tmp-11.md
git mv 10-jwks-deep-dive.md tmp-12.md
git mv 11-access-token-refresh-token-lifecycle.md tmp-13.md
git mv 08-request-response-details.md tmp-14.md
```

- [ ] **Step 2: Phase two — move temp names to final names**

```bash
git mv tmp-04.md 04-local-demo-guide.md
git mv tmp-05.md 05-component-tour.md
git mv tmp-06.md 06-keycloak-idp.md
git mv tmp-07.md 07-kong.md
git mv tmp-08.md 08-opa.md
git mv tmp-09.md 09-banking-api-service.md
git mv tmp-11.md 11-jwt-signature-validation.md
git mv tmp-12.md 12-jwks.md
git mv tmp-13.md 13-token-lifecycle.md
git mv tmp-14.md 14-request-response-reference.md
```

- [ ] **Step 3: Verify the final layout**

Run:

```bash
ls [0-9][0-9]-*.md | sort
```

Expected: exactly `01..14` with the names from the file map, no `tmp-*`, no duplicate numbers, no `08-request-response-details.md`/`09-opa-integration.md`/etc. remaining.

```bash
ls [0-9][0-9]-*.md | sed -E 's/-.*//' | sort | uniq -d
```

Expected: empty output (no duplicate numbers).

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "docs: renumber files into four-part layout"
```

---

## Task 2: Rewrite `01-concepts.md` (Part I) + add the canonical glossary

**Files:**
- Modify: `docs/01-concepts.md`

- [ ] **Step 1: Apply the standard template**

- Title becomes `# 01 — Concepts`.
- Add the one-line "what this is" sentence.
- No Prereqs line content needed (it's the first doc) — use `> **Part I · Foundations** — Start here.`
- Footer: no Prev; `Next: [02 — This Project Architecture](02-this-project-architecture.md) →`.

- [ ] **Step 2: Make this the single owner of the glossary**

Keep the existing concept sections (Authn vs Authz, IdP, JWT, PEP, PDP, why separate, concept map). Add a short **Glossary** section near the top that maps each abstract role to this PoC's concrete component, so every later doc can link here instead of redefining:

```markdown
## Glossary

- **IdP (Identity Provider)** — issues identity + tokens. Here: `Keycloak`.
- **PEP (Policy Enforcement Point)** — intercepts requests, enforces decisions. Here: `Kong`.
- **PDP (Policy Decision Point)** — decides allow/deny from policy. Here: `OPA`.
- **Resource server** — owns the protected data, re-checks the token. Here: `banking-api-service`.
- **JWT** — signed token carrying identity + claims.
```

- [ ] **Step 3: Normalize examples to personas**

Where examples appear, use `alice` / `ops-admin` only.

- [ ] **Step 4: Verify**

Run the verification idiom for `01-concepts.md` (nav OK, personas OK, links checked) and:

```bash
grep -q '## Glossary' 01-concepts.md && echo "glossary OK"
```

- [ ] **Step 5: Commit**

```bash
git add 01-concepts.md && git commit -m "docs: rewrite 01 concepts with glossary and template"
```

---

## Task 3: Rewrite `02-this-project-architecture.md` (Part I)

**Files:**
- Modify: `docs/02-this-project-architecture.md`

- [ ] **Step 1: Apply the standard template**

- Title `# 02 — This Project Architecture`.
- Breadcrumb `> **Part I · Foundations** — Prereqs: [01](01-concepts.md)`.
- Footer: `← Prev: [01 — Concepts](01-concepts.md) · Next: [03 — Request Flows](03-request-flows.md) →`.

- [ ] **Step 2: Align component descriptions with the glossary roles**

For each of `Keycloak`/`Kong`/`OPA`/`banking-api-service`/`identity-bootstrap-service`, state the role in glossary terms (IdP/PEP/PDP/resource server/demo setup) and link `01`. Keep the architecture diagram; ensure Mermaid node labels match the locked names. Keep the file/project mapping section but verify paths against the repo (`infra/keycloak/realm-export.json`, `infra/kong/kong.yml`, `infra/opa/policies/`, `services/banking-api-service`, `services/identity-bootstrap-service`).

- [ ] **Step 3: Verify**

Run the verification idiom for `02-this-project-architecture.md`, plus confirm referenced repo paths exist:

```bash
for p in infra/keycloak/realm-export.json infra/kong/kong.yml infra/opa/policies services/banking-api-service services/identity-bootstrap-service; do test -e "../$p" || echo "MISSING $p"; done; echo "paths checked"
```

- [ ] **Step 4: Commit**

```bash
git add 02-this-project-architecture.md && git commit -m "docs: rewrite 02 architecture with template"
```

---

## Task 4: Rewrite `03-request-flows.md` (Part I)

**Files:**
- Modify: `docs/03-request-flows.md`

- [ ] **Step 1: Apply the standard template**

- Title `# 03 — Request Flows`.
- Breadcrumb `> **Part I · Foundations** — Prereqs: [01](01-concepts.md), [02](02-this-project-architecture.md)`.
- Footer: `← Prev: [02 — This Project Architecture](02-this-project-architecture.md) · Next: [04 — Local Demo Guide](04-local-demo-guide.md) →`.

- [ ] **Step 2: Normalize the flows to personas and add forward links**

Keep the five flows (setup, login, allowed access, forbidden access, missing/tampered token). Ensure the allowed/forbidden flows use `alice` (own vs other account) and `ops-admin`. Add a one-line pointer at the end of each flow to the component or reference doc that covers it in depth (e.g. introspection → `11`, OPA decision → `08`, wire payloads → `14`).

- [ ] **Step 3: Verify**

Run the verification idiom for `03-request-flows.md`.

- [ ] **Step 4: Commit**

```bash
git add 03-request-flows.md && git commit -m "docs: rewrite 03 request flows with template"
```

---

## Task 5: Rewrite `04-local-demo-guide.md` (Part I)

**Files:**
- Modify: `docs/04-local-demo-guide.md`

- [ ] **Step 1: Apply the standard template**

- Title `# 04 — Local Demo Guide`.
- Breadcrumb `> **Part I · Foundations** — Prereqs: [02](02-this-project-architecture.md), [03](03-request-flows.md)`.
- Footer: `← Prev: [03 — Request Flows](03-request-flows.md) · Next: [05 — Component Tour](05-component-tour.md) →`.

- [ ] **Step 2: Verify commands against the repo**

Confirm the demo script path and compose-driven endpoints match reality before keeping them: `scripts/demo.sh` exists; ports in `docker-compose.yml` are Keycloak `9081`, Kong proxy `8000` / admin `8001`, OPA `8181`. Fix any drifted port/path/command in the doc.

- [ ] **Step 3: Verify**

Run the verification idiom for `04-local-demo-guide.md`, plus:

```bash
test -f ../scripts/demo.sh && echo "demo script OK"
grep -q '9081\|8000\|8001\|8181' 04-local-demo-guide.md && echo "ports referenced"
```

- [ ] **Step 4: Commit**

```bash
git add 04-local-demo-guide.md && git commit -m "docs: rewrite 04 demo guide with template"
```

---

## Task 6: Rewrite `05-component-tour.md` (Part II opener)

**Files:**
- Modify: `docs/05-component-tour.md`

- [ ] **Step 1: Apply the standard template**

- Title `# 05 — Component Tour`.
- Breadcrumb `> **Part II · Component Deep Dives** — Prereqs: [02](02-this-project-architecture.md)`.
- Footer: `← Prev: [04 — Local Demo Guide](04-local-demo-guide.md) · Next: [06 — Keycloak / IdP](06-keycloak-idp.md) →`.

- [ ] **Step 2: Frame it as the map of Part II**

Keep the one-paragraph-per-component summaries (they are the intentional shallow bridge, not duplication). At the end of each component blurb, link to its deep-dive doc: Keycloak → `06`, Kong → `07`, OPA → `08`, banking-api-service → `09`, identity-bootstrap-service → `10`. Keep the claim/decision Mermaid diagram with locked node labels.

- [ ] **Step 3: Verify**

Run the verification idiom for `05-component-tour.md`, plus confirm all five deep-dive links are present:

```bash
for f in 06-keycloak-idp 07-kong 08-opa 09-banking-api-service 10-identity-bootstrap-service; do grep -q "$f.md" 05-component-tour.md || echo "missing link $f"; done; echo "tour links checked"
```

- [ ] **Step 4: Commit**

```bash
git add 05-component-tour.md && git commit -m "docs: rewrite 05 component tour as Part II map"
```

---

## Task 7: Rewrite `06-keycloak-idp.md` — focus on Keycloak; move generic theory out

**Files:**
- Modify: `docs/06-keycloak-idp.md`
- (Consolidation targets used later: `11-jwt-signature-validation.md`, `13-token-lifecycle.md`)

- [ ] **Step 1: Apply the standard template**

- Title `# 06 — Keycloak / IdP`.
- Breadcrumb `> **Part II · Component Deep Dives** — Prereqs: [01](01-concepts.md), [05](05-component-tour.md)`.
- Footer: `← Prev: [05 — Component Tour](05-component-tour.md) · Next: [07 — Kong](07-kong.md) →`.

- [ ] **Step 2: Keep Keycloak-specific content**

Keep: What an IdP is (brief, link `01`), why this project needs one, where Keycloak sits, realm/users/roles/clients/protocol mappers/tokens, how Keycloak is configured in this repo (verify against `infra/keycloak/realm-export.json`), how it interoperates with each component, the `alice`/`ops-admin` examples, common misunderstandings.

- [ ] **Step 3: Move generic token/session/introspection theory out**

The current file (post-rename `06-keycloak-idp.md`) carries ~250 lines of generic theory in sections such as "How Keycloak Tracks Sessions And Token Activity", "Access Token vs Session State", "What Keycloak Usually Keeps Server-Side", "What Happens During Introspection", "Why Keycloak Can Judge Whether A Token Is Still Active", "How Access Token And Refresh Token Relate To Session State", "Why Keycloak Can Return `active: false`".

- The **introspection mechanics** belong to `11`. In `06`, keep only a short "Keycloak owns the live session truth, which is why introspection can answer `active:false`" paragraph and link `11`.
- The **access/refresh lifecycle** belongs to `13`. In `06`, keep only the Keycloak-specific session storage detail (logical session types + Infinispan/offline storage) and link `13` for the lifecycle.
- Cut text that is duplicated verbatim once the pointer is in place.

Stash the removed prose where the executor can paste it into Tasks 12 and 14 (Part III). Practical method:

```bash
# capture the sections being relocated for reuse in Tasks 12/14
sed -n '/## How Keycloak Tracks Sessions/,/## How Keycloak Is Configured In This Repo/p' 06-keycloak-idp.md > /tmp/06-relocated.md
```

- [ ] **Step 4: Verify**

Run the verification idiom for `06-keycloak-idp.md`, plus confirm the handoff links exist and the file shrank:

```bash
grep -q '11-jwt-signature-validation.md' 06-keycloak-idp.md && grep -q '13-token-lifecycle.md' 06-keycloak-idp.md && echo "handoff links OK"
test -f /tmp/06-relocated.md && echo "relocated text captured"
```

- [ ] **Step 5: Commit**

```bash
git add 06-keycloak-idp.md && git commit -m "docs: refocus 06 on Keycloak, relocate generic token theory"
```

---

## Task 8: Rewrite `07-kong.md` (Part II)

**Files:**
- Modify: `docs/07-kong.md`

- [ ] **Step 1: Apply the standard template**

- Title `# 07 — Kong (Gateway / PEP)`.
- Breadcrumb `> **Part II · Component Deep Dives** — Prereqs: [01](01-concepts.md), [05](05-component-tour.md)`.
- Footer: `← Prev: [06 — Keycloak / IdP](06-keycloak-idp.md) · Next: [08 — OPA](08-opa.md) →`.

- [ ] **Step 2: Keep Kong-specific content; defer introspection mechanics**

Keep: what Kong is (PEP, link `01`), the two config files (`docker-compose.yml`, `infra/kong/kong.yml`), service/route/plugin config, the `opa-authz` plugin `schema.lua`/`handler.lua` walkthrough (verify against `infra/kong/plugins/opa-authz/`), the Kong request flow, interop sections, inspection commands. For "Why Kong uses introspection instead of JWKS," keep the *Kong-side rationale* but defer the introspection mechanics to `11` with a link. Use `alice`/`ops-admin` in examples.

- [ ] **Step 3: Verify**

Run the verification idiom for `07-kong.md`, plus:

```bash
for p in infra/kong/kong.yml infra/kong/plugins/opa-authz/handler.lua infra/kong/plugins/opa-authz/schema.lua; do test -f "../$p" || echo "MISSING $p"; done
grep -q '11-jwt-signature-validation.md' 07-kong.md && echo "introspection link OK"
```

- [ ] **Step 4: Commit**

```bash
git add 07-kong.md && git commit -m "docs: rewrite 07 Kong with template, defer introspection to 11"
```

---

## Task 9: Rewrite `08-opa.md` (Part II)

**Files:**
- Modify: `docs/08-opa.md`

- [ ] **Step 1: Apply the standard template**

- Title `# 08 — OPA (PDP)`.
- Breadcrumb `> **Part II · Component Deep Dives** — Prereqs: [01](01-concepts.md), [07](07-kong.md)`.
- Footer: `← Prev: [07 — Kong](07-kong.md) · Next: [09 — banking-api-service](09-banking-api-service.md) →`.

- [ ] **Step 2: Keep OPA content; verify Rego against the repo**

Keep: what OPA is (PDP, link `01`), why this project needs it, where it sits, Rego basics, deny-by-default, the actual policy walkthrough, the input contract, how Kong sends input, what OPA returns, the tests, examples. Verify every Rego snippet, rule name, and the test cases against `infra/opa/policies/banking_authz.rego` and `infra/opa/policies/banking_authz_test.rego` and fix any drift. For "what input OPA receives," list only the claims OPA consumes and link `14` for the full claim catalog. Examples use `alice`/`ops-admin`.

- [ ] **Step 3: Verify**

Run the verification idiom for `08-opa.md`, plus:

```bash
for p in infra/opa/policies/banking_authz.rego infra/opa/policies/banking_authz_test.rego; do test -f "../$p" || echo "MISSING $p"; done
grep -q '14-request-response-reference.md' 08-opa.md && echo "claim catalog link OK"
```

- [ ] **Step 4: Commit**

```bash
git add 08-opa.md && git commit -m "docs: rewrite 08 OPA with template, verify Rego"
```

---

## Task 10: Rewrite `09-banking-api-service.md` (Part II)

**Files:**
- Modify: `docs/09-banking-api-service.md`

- [ ] **Step 1: Apply the standard template**

- Title `# 09 — banking-api-service (Resource Server)`.
- Breadcrumb `> **Part II · Component Deep Dives** — Prereqs: [01](01-concepts.md), [05](05-component-tour.md)`.
- Footer: `← Prev: [08 — OPA](08-opa.md) · Next: [10 — identity-bootstrap-service](10-identity-bootstrap-service.md) →`.

- [ ] **Step 2: Keep service content; verify against Java source; defer mechanics**

Keep: what the service does, why it re-checks security, the request flow, `SecurityConfig` wiring, `JwtDecoder`, JWT validation (signature/issuer/audience), `AccountAccessGuard` rules, controllers, interop, examples. Verify class/method names and config keys against `services/banking-api-service/src/main/.../security/SecurityConfig` and `application.yml` (`BANKING_API_JWK_SET_URI`, `BANKING_API_ISSUER_URI`, `BANKING_API_AUDIENCE` per `docker-compose.yml`). For "uses JWKS not introspection," keep the resource-server rationale and link `11` (decision) and `12` (JWKS mechanics). List only claims the service reads; link `14` for the catalog. Examples use `alice`/`ops-admin`.

- [ ] **Step 3: Verify**

Run the verification idiom for `09-banking-api-service.md`, plus:

```bash
grep -rq 'SecurityConfig' ../services/banking-api-service/src/main && echo "SecurityConfig exists"
grep -q '11-jwt-signature-validation.md' 09-banking-api-service.md && grep -q '12-jwks.md' 09-banking-api-service.md && echo "mechanics links OK"
```

- [ ] **Step 4: Commit**

```bash
git add 09-banking-api-service.md && git commit -m "docs: rewrite 09 banking-api-service with template"
```

---

## Task 11: Create `10-identity-bootstrap-service.md` (new, Part II)

**Files:**
- Create: `docs/10-identity-bootstrap-service.md`
- Source material: the identity-bootstrap blurb (now in `05-component-tour.md`), the admin-API flows in `14-request-response-reference.md`, and the Java source under `services/identity-bootstrap-service/src/main`.

- [ ] **Step 1: Write the doc with the standard template**

Structure:

```markdown
# 10 — identity-bootstrap-service (Demo Setup)

A small internal service that seeds demo users into Keycloak so the PoC is runnable. It is not real customer onboarding.

> **Part II · Component Deep Dives** — Prereqs: [03](03-request-flows.md), [06](06-keycloak-idp.md)

## What it does
- Creates demo-managed users in `Keycloak` (`alice`, `ops-admin`)
- Sets passwords, `customer_id`, `account_ids`, and demo roles
- Runs only inside the Compose network (not exposed publicly)

## How it talks to Keycloak
(Describe the Keycloak Admin API usage; verify class names against
services/identity-bootstrap-service/src/main/java/.../user/*.java —
KeycloakAdminProvisioner, KeycloakUserProvisioner, DemoUserService, DemoUserController.)

## Worked example: provisioning `alice`
(Use the DemoUserRequest body shape; link [14](14-request-response-reference.md) for full wire payloads.)

## Where it fits
- Triggered in Flow 1 of [03](03-request-flows.md)
- Produces the users whose tokens drive [06](06-keycloak-idp.md) onward

## What to remember
(2-3 line recap.)

---

← Prev: [09 — banking-api-service](09-banking-api-service.md) · Next: [11 — JWT Signature, Validation & Introspection](11-jwt-signature-validation.md) →
```

- [ ] **Step 2: Verify class/endpoint names against source**

```bash
ls ../services/identity-bootstrap-service/src/main/java/com/banking/poc/identitybootstrap/user/
```

Expected to include `KeycloakAdminProvisioner.java`, `KeycloakUserProvisioner.java`, `DemoUserService.java`, `DemoUserController.java`, `DemoUserRequest.java`. Ensure the doc references only names that appear here.

- [ ] **Step 3: Verify**

Run the verification idiom for `10-identity-bootstrap-service.md`.

- [ ] **Step 4: Commit**

```bash
git add 10-identity-bootstrap-service.md && git commit -m "docs: add 10 identity-bootstrap-service deep dive"
```

---

## Task 12: Rewrite `11-jwt-signature-validation.md` — own introspection mechanics

**Files:**
- Modify: `docs/11-jwt-signature-validation.md`
- Source of relocated text: `/tmp/06-relocated.md` (from Task 7)

- [ ] **Step 1: Apply the standard template**

- Title `# 11 — JWT Signature, Validation & Introspection`.
- Breadcrumb `> **Part III · Token Mechanics** — Prereqs: [06](06-keycloak-idp.md), [07](07-kong.md)`.
- Footer: `← Prev: [10 — identity-bootstrap-service](10-identity-bootstrap-service.md) · Next: [12 — JWKS Deep Dive](12-jwks.md) →`.

- [ ] **Step 2: Become the single owner of introspection mechanics**

Keep the four pieces (signature, validation, introspection, claims) and the trust chain. Fold in the relevant relocated introspection theory from `/tmp/06-relocated.md` (what introspection checks, why Keycloak can answer `active:false`) so this is the one full treatment. Keep the JWKS-vs-introspection *decision* here but defer JWKS key mechanics to `12` with a link. Examples use `alice`/`ops-admin`.

- [ ] **Step 3: Verify**

Run the verification idiom for `11-jwt-signature-validation.md`, plus:

```bash
grep -q '12-jwks.md' 11-jwt-signature-validation.md && echo "jwks link OK"
grep -qi 'active.*false' 11-jwt-signature-validation.md && echo "introspection theory present"
```

- [ ] **Step 4: Commit**

```bash
git add 11-jwt-signature-validation.md && git commit -m "docs: make 11 the owner of introspection mechanics"
```

---

## Task 13: Rewrite `12-jwks.md` (Part III)

**Files:**
- Modify: `docs/12-jwks.md`

- [ ] **Step 1: Apply the standard template**

- Title `# 12 — JWKS Deep Dive`.
- Breadcrumb `> **Part III · Token Mechanics** — Prereqs: [11](11-jwt-signature-validation.md)`.
- Footer: `← Prev: [11 — JWT Signature, Validation & Introspection](11-jwt-signature-validation.md) · Next: [13 — Access & Refresh Token Lifecycle](13-token-lifecycle.md) →`.

- [ ] **Step 2: Confirm sole ownership of JWKS / key-by-`kid`**

Keep the full JWKS treatment (definitions, why it exists, the actual JWKS response in this PoC, the two keys, JWK anatomy, header-to-JWK match by `kid`, rotation, Spring behavior, inspection, failure modes). Verify the `jwk-set-uri` value matches `BANKING_API_JWK_SET_URI` in `docker-compose.yml`. Ensure `11` defers here (added in Task 12). Examples use `alice`/`ops-admin` where relevant.

- [ ] **Step 3: Verify**

Run the verification idiom for `12-jwks.md`, plus:

```bash
grep -q 'protocol/openid-connect/certs' 12-jwks.md && echo "jwks uri OK"
```

- [ ] **Step 4: Commit**

```bash
git add 12-jwks.md && git commit -m "docs: rewrite 12 JWKS with template"
```

---

## Task 14: Rewrite `13-token-lifecycle.md` — own access/refresh lifecycle

**Files:**
- Modify: `docs/13-token-lifecycle.md`
- Source of relocated text: `/tmp/06-relocated.md` (from Task 7)

- [ ] **Step 1: Apply the standard template**

- Title `# 13 — Access & Refresh Token Lifecycle`.
- Breadcrumb `> **Part III · Token Mechanics** — Prereqs: [11](11-jwt-signature-validation.md)`.
- Footer: `← Prev: [12 — JWKS Deep Dive](12-jwks.md) · Next: [14 — Request & Response Details](14-request-response-reference.md) →`.

- [ ] **Step 2: Become the single owner of token/session lifecycle**

Keep the lifecycle content (fields, why they exist, refresh problem, renewal flow, client pseudocode, failure cases, security). Fold in the relocated *lifecycle* portions from `/tmp/06-relocated.md` (access vs refresh vs session relationship) so this is the one full treatment; `06` only links here. Examples use `alice`/`ops-admin`.

- [ ] **Step 3: Verify**

Run the verification idiom for `13-token-lifecycle.md`, plus confirm `06` points here:

```bash
grep -q '13-token-lifecycle.md' 06-keycloak-idp.md && echo "06->13 link OK"
```

- [ ] **Step 4: Commit**

```bash
git add 13-token-lifecycle.md && git commit -m "docs: make 13 the owner of token lifecycle"
```

---

## Task 15: Rewrite `14-request-response-reference.md` — own the claim catalog & wire payloads

**Files:**
- Modify: `docs/14-request-response-reference.md`

- [ ] **Step 1: Apply the standard template**

- Title `# 14 — Request & Response Details (Wire-Level Reference)`.
- Breadcrumb `> **Part IV · Reference** — Use as a lookup; read Parts I–III first.`
- Footer: `← Prev: [13 — Access & Refresh Token Lifecycle](13-token-lifecycle.md)` (no Next).

- [ ] **Step 2: Frame as the reference and own the claim catalog**

Keep the per-hop request/response detail. Make the JWT claim catalog (the `Flow 5*` material) the canonical catalog that `06`/`08`/`09` link to. Add a short "How to use this doc" intro clarifying it is a lookup, not a read-through. Verify representative payloads (claims, introspection response, OPA input/result, forwarded headers) against `infra/keycloak/realm-export.json`, `infra/opa/policies/`, and `infra/kong/kong.yml`. Examples use `alice`/`ops-admin`.

- [ ] **Step 3: Verify**

Run the verification idiom for `14-request-response-reference.md` (note: no Next link, so adjust the nav grep to require only `Prev:`).

```bash
grep -q '^> \*\*Part IV' 14-request-response-reference.md && grep -q 'Prev:' 14-request-response-reference.md && echo "nav OK"
```

- [ ] **Step 4: Commit**

```bash
git add 14-request-response-reference.md && git commit -m "docs: rewrite 14 as wire-level reference and claim catalog"
```

---

## Task 16: Rewrite `README.md` as the map

**Files:**
- Modify: `docs/README.md`

- [ ] **Step 1: Write the new README**

Replace the body with:

```markdown
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
- `01-concepts.md` — authn/authz, IdP, PEP, PDP, JWT (start here)
- `02-this-project-architecture.md` — how the concepts map to components
- `03-request-flows.md` — the end-to-end stories
- `04-local-demo-guide.md` — run it and watch it work

### Part II — Component Deep Dives
- `05-component-tour.md` — one-paragraph map of all five components
- `06-keycloak-idp.md` — the IdP that issues tokens
- `07-kong.md` — the gateway / PEP and its OPA plugin
- `08-opa.md` — the PDP and its Rego policy
- `09-banking-api-service.md` — the resource server that re-validates
- `10-identity-bootstrap-service.md` — demo user setup

### Part III — Token Mechanics
- `11-jwt-signature-validation.md` — signature, validation, introspection
- `12-jwks.md` — JWK/JWKS and key selection by `kid`
- `13-token-lifecycle.md` — access/refresh tokens and renewal

### Part IV — Reference
- `14-request-response-reference.md` — wire-level headers, bodies, claims

## Where to start
- New to the topic → `01-concepts.md`
- Want the system fast → `02` + `03` + `04`
- Need wire-level payloads → `14-request-response-reference.md`

## Design and planning docs
- `superpowers/specs/2026-06-18-docs-reorganization-design.md`
- `superpowers/plans/2026-06-18-docs-reorganization.md`
- `superpowers/specs/2026-06-05-mobile-banking-auth-design.md`
- `superpowers/plans/2026-06-05-mobile-banking-auth-poc.md`
```

- [ ] **Step 2: Verify every listed doc exists**

```bash
for f in $(grep -oE '`[0-9]{2}-[a-z-]+\.md`' README.md | tr -d '`'); do test -f "$f" || echo "MISSING $f"; done; echo "README links checked"
```

Expected: no `MISSING` lines.

- [ ] **Step 3: Commit**

```bash
git add README.md && git commit -m "docs: rewrite README as the four-part reading map"
```

---

## Task 17: Final whole-series verification pass

**Files:** all of `docs/*.md`.

- [ ] **Step 1: Numbering is exactly 01..14, unique**

```bash
cd /Users/phil/ML/micro-service-auth/docs
ls [0-9][0-9]-*.md | sed -E 's/-.*//' | sort | tr '\n' ' '; echo
ls [0-9][0-9]-*.md | sed -E 's/-.*//' | sort | uniq -d
```

Expected: `01 02 03 04 05 06 07 08 09 10 11 12 13 14` and empty duplicate output.

- [ ] **Step 2: Every doc has breadcrumb + nav**

```bash
for f in [0-9][0-9]-*.md; do grep -q '^> \*\*Part' "$f" || echo "no breadcrumb: $f"; grep -q 'Prev:\|Next:\|Start here\|read Parts' "$f" || echo "no nav: $f"; done; echo "breadcrumb/nav checked"
```

- [ ] **Step 3: No broken relative doc links anywhere**

```bash
for f in [0-9][0-9]-*.md README.md; do for l in $(grep -oE '\(([0-9]{2}-[a-z-]+\.md)' "$f" | tr -d '('); do test -f "$l" || echo "BROKEN in $f: $l"; done; done; echo "all links checked"
```

Expected: no `BROKEN` lines.

- [ ] **Step 4: No leftover references to the old filenames**

```bash
grep -rnE 'idp-keycloak-deep-dive|kong-integration|opa-integration|banking-api-service-authn-authz|jwks-deep-dive|access-token-refresh-token-lifecycle|request-response-details|component-deep-dives|05-local-demo-guide' . --include='*.md' | grep -v superpowers/ ; echo "old-name check done"
```

Expected: no output (outside `superpowers/` plan/spec docs).

- [ ] **Step 5: Personas only — no stray example names**

```bash
grep -rniE '\b(bob|carol|charlie|john|jane|dave)\b' [0-9][0-9]-*.md ; echo "persona check done"
```

Expected: no output (or only legitimate non-name matches, reviewed by hand).

- [ ] **Step 6: Commit any fixes from this pass**

```bash
git add -A && git commit -m "docs: final verification fixes for reorganized series" || echo "nothing to fix"
```

---

## Self-Review Notes (author)

- **Spec coverage:** target structure (Tasks 1–16), per-doc template (every rewrite task), navigation/README (Tasks 6/16, all footers), dedup ownership map (Tasks 7/12/13/14/15), new identity-bootstrap doc (Task 11), repo-verification guardrail (verify steps in Tasks 3,5,7,8,9,10,11,13,15), acceptance criteria (Task 17). All covered.
- **Placeholders:** none — every nav string, link, and command is concrete.
- **Consistency:** final filenames, breadcrumb format, and footer format are identical across tasks; relocated-text handoff uses one temp file `/tmp/06-relocated.md` referenced by Tasks 7→12/14.
