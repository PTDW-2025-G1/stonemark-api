# StoneMark API — Stabilization Plan

> Simplified working doc. Scope: `stonemark-api` only. Baseline: `428bb816` (2026-06-30).

## Goal & priority

**Objective:** receive, preprocess, manage, categorize, publish masonic marks.

**There is no labelled data yet**, so detection, embedding, similarity and auto-approval are not usable. The V1 goal is therefore:

> **Launch simple, low-friction submissions to start collecting data.**

Sequence: simple intake → manual moderation (= labelling) → train → automate.

**Deferred:** deployment, migrations, public read API, ML/auto-approval.

## Status

- **P1 — Launch-ready intake: done.**
- **P2 — Manual moderation + labelling: done.**
- **P4 — Consolidate modules: done** (structural; behavioral de-engineering deferred).
- **P5 — Pipeline tests + CI: done.**
- **P3 — Train + automate: not started** (gated on collected data).
- Behavior changes: the AI pipeline is off by default (`processing.enabled=false`), and the API is **secure by default** (`application.security.disabled=false`).

## V1 scope

- Submit: **photo (required) + location (optional) + notes (optional)**, no account required.
- Store the submission and queue it for manual moderation.
- Moderate: assign to a mark / create a mark / reject — this produces the training labels.
- Vision is **not** in the critical path (see `marks-processing/PLAN.md`).

## What blocks V1

| # | Problem | Where | Fix |
|---|---|---|---|
| 1 | Intake is non-atomic — file staged inside a DB transaction; rollback orphans the file and can lose submissions | `intake/.../SubmissionFacade.java` | stage → commit with compensation; make submission creation idempotent |
| 2 | Submission is coupled to processing, which will emit failing/deferred records without usable vision | `processing/.../MarkEvidenceSubmittedListener.java` | decouple; disable processing in V1 |
| 3 | Two public upload paths (`/submissions` and `/public/media`) | `intake`, `file/.../MediaController.java` | one path for submissions |
| 4 | Public media upload/delete open to abuse | `file/.../MediaController.java` | restrict writes |
| 5 | Security disabled by default | `application-security.yml` | enable; don't ship `disabled: true` |
| 6 | No tests on the intake path | — | one end-to-end smoke test |

## Plan

**P1 — Launch-ready intake. ✅ Done.**
- Submission + media now commit in **one transaction**; object storage is compensated on rollback (no orphaned files/rows).
- **Idempotent**: optional `Idempotency-Key` header (unique column); replays return the existing submission.
- **Decoupled from vision**: `processing.enabled` (default `false`) gates the listener, schedulers, async executor and processing endpoints.
- Forgiving validation: photo only required; empty file rejected with a clear 400; staging + reverse-geocoding run **outside** the DB transaction; abandoned staged files are swept.
- Public media **write** endpoints locked to moderator/admin; `@EnableMethodSecurity` added so `@PreAuthorize` actually applies; Google login made optional so the app boots without OAuth creds.
- Boot fixes: removed the broken `@EntityGraph`; fixed two no-op self-invoked `@Transactional` methods.
- Test: `SubmissionFacadeTest`.
*Done: a phone photo submits successfully with no account and no location, is stored exactly once, and never depends on vision.*

**P2 — Manual moderation + labelling. ✅ Done.**
- Review/moderation now works **without the AI pipeline**: `ReviewService` treats a processing record as optional (no more "not processed" failure).
- Approving links the photo to a mark occurrence even with **no embedding**; a linking failure now rolls the whole decision back, so a submission cannot be approved with unlinked evidence.
- Every decision is persisted as a `MarkEvidenceReview` label (submission → existing mark / new mark / reject) with reviewer and comment.
- Queue: `GET /api/v1/admin/submissions?status=RECEIVED`. Decisions: `POST /api/v1/admin/review/{id}/accept|accept-as-new|reject`. Labels: `GET /api/v1/admin/review`.
- Admin bootstrap: first admin created from `APPLICATION_BOOTSTRAP_ADMIN_USERNAME` / `_PASSWORD` / `_EMAIL` (disabled when unset).
- Test: `ReviewServiceTest` (approve and reject without processing).
*Done: a moderator can clear the queue and each decision is recorded as training data.*

**P3 — Collect, then automate.** Grow the labelled set, train the detector/embedder, evaluate, enable suggestions, then auto-approve. See `marks-processing/PLAN.md`.
*Done: suggestions appear, then the confident cases resolve with no human clicks.*

**P4 — Consolidate modules. ✅ Done (structure).**
- Folded the four contract jars into their implementations: `file-api`→`file`, `mark-api`→`mark`, `user-api`→`user`, `intake-api`→`intake` (same packages, pure moves).
- Merged `common-core` + `common-infra` + `common-web` into one `shared` module; packages renamed to `pt.estga.shared.core` / `.jpa` / `.web`.
- Removed dead WhatsApp wiring (config + security matcher) — no code ever existed for it.
- **22 → 16 Maven modules**; one Maven module per capability.
- **Deferred (behavioral/product, needs a decision):** cutting the `file` multi-variant pipeline, simplifying the `chatbot` engine, consolidating the `processing` schedulers/semaphore, and making `modules.verify()` fail the build.

**P5 — Prove it. ✅ Done.**
- Pipeline tests with a **stubbed Vision client** and a seeded (mocked) evidence corpus:
  - `SimilarityServiceTest` — groups evidence hits by mark, sums and sorts confidence; empty embedding and non-finite scores yield no suggestions.
  - `ProcessingServiceImplTest` — Vision unavailable → `PENDING`; already-processed → skip; missing media → permanent `FAILED`; empty embedding → permanent `FAILED`; happy path → embedding + suggestions into `finalizeSuccess`.
- Minimal CI: `.github/workflows/ci.yml` runs `./mvnw -B clean test` on JDK 25 and uploads surefire reports.

## Overengineering to cut (later)

See **[`BEHAVIORAL-DEENGINEERING.md`](BEHAVIORAL-DEENGINEERING.md)** for the full plan (options, impact, risks, decisions). Summary of what remains:

| Subsystem | Keep | Cut |
|---|---|---|
| `file` image pipeline | storage (local filesystem) + staging + original | multi-variant async pipeline, recovery job, retry/`Thread.sleep` |
| `processing` scaffolding | the pipeline shape | two schedulers → one; hand-rolled semaphore → bounded executor; dead scoring config |
| `chatbot` | minimal photo+location ingestion (or park) | conversational engine, UI framework, i18n |
| module system | Modulith | swallowed `modules.verify()` |

These are **behavioral/product decisions** — not done blind. WhatsApp config was already removed.

## Module set (13)

`shared`, `notification`, `file`, `monument`, `user`, `mark`, `support`, `bookmark`, `intake`, `processing`, `review`, `application`, `auth`. One capability = one Maven module.

Joins completed: `*-api` → domains, `common-*` → `shared`, `vision` → `processing`, `verification` → `user`.
**Removed:** `chatbot` (Telegram ingestion) — never used in production; recoverable from git history.
**Still to join (D3):** `notification` → `support`.

## Open decisions

1. `chatbot` — simplify into `intake` vs park (delete WhatsApp either way).
2. `file` derivatives — 3 / 1 / none (recommend 1).
3. Moderation model — who approves, and whether new marks are created directly or held provisional.

## Notes

- Build + tests verified: `mvn -B clean test`, 13 modules, **BUILD SUCCESS**, 46 tests, 0 failures (Docker; no local JDK).
- **Chatbot removed** (Telegram ingestion); the app needs no Telegram config.
- **CI:** `.github/workflows/ci.yml` runs the build + tests on JDK 25.
- **Switches:** `processing.enabled=false` disables the AI pipeline (set to `true` only once a trained model exists). `application.security.disabled=false` is the secure default; set it to `true` for local runs without auth.
- Unused-but-referenced code is not deleted on sight; only genuinely broken pieces are fixed.
- Automatic approval is **gated on data** — it is P3, not P1.
- The Modulith verification test still only *prints* violations (informational); enforcing it is deferred until the remaining boundary violations are routed through the `.api` packages.
- Security is on, so admin/moderation endpoints require authentication; the first admin is provisioned by the bootstrap seeder (`APPLICATION_BOOTSTRAP_ADMIN_USERNAME` / `_PASSWORD`).
