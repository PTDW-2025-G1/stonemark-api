# Domain Model & API — Target Design and Work Plan

> Scope: `stonemark-api`. UI is disposable at this stage, so the model and HTTP surface may change freely.
> Enforced guardrail: `modules.verify()` fails the build on boundary violations. Every commit below must leave the build green.

## 0. How to use this document

- §1 is the critique of what exists. §2–§3 are the target. §4 lists the decisions that shape §2/§3.
- §5 is the work plan as an ordered list of commits. **C4 onward depends on §4 being decided.**
- Nothing here is implemented yet.

---

## 1. Current state

### 1.1 Data model problems

| # | Problem | Where |
|---|---|---|
| M1 | **Place is optional and effectively discarded.** `MarkOccurrence.monumentId` is nullable and review creates occurrences with `monumentId = null`. The submission's lat/lon and `Monument.location` are never connected. | `MarkOccurrence.java`, `ReviewService.findOrCreateOccurrence` |
| M2 | **Two overlapping lifecycles.** `Mark`/`MarkOccurrence` extend `BaseEntity` (`status`, soft delete) **and** declare `validationState` (PROVISIONAL/VERIFIED). No first-class published state/timestamp; `MarkOccurrenceDto.publishedAt` is never set. | `Mark.java`, `MarkOccurrence.java` |
| M3 | **Value-only cross-module refs with no FKs.** `MarkEvidence.fileId`, `Occurrence.monumentId`, `Submission.originalMediaFileId`/`submittedById`, `Review.selectedMarkId`. Orphans possible; the unique `(mark_id, monument_id)` on a nullable column does not prevent duplicates (Postgres NULLs are distinct). | `MarkOccurrence.java` |
| M4 | **Circular evidence links and lost lineage.** `Mark.exemplar → MarkEvidence` while `MarkEvidence → MarkOccurrence → Mark`; the approved evidence keeps no link back to its `Submission` (only `fileId`). | `Mark.java`, `MarkEvidence.java` |
| M5 | **Inconsistent lifecycle base.** Some entities extend `BaseEntity` (audit + soft delete), `MediaFile` extends `CreationAuditedEntity`, and `Submission`/`Review`/`Bookmark`/`ContactRequest` extend nothing. | across modules |
| M6 | **Abandoned home-grown geo lookup.** Division code/name is resolved by calling a self-built geo API (`geo.api.base-url`, default `http://localhost:8081`). The service does not exist, so calls fail silently and resolve to `null` — leaving `divisionCode` plumbing (entities, DTOs, filters, an endpoint) coupled to a dead idea. | `shared/jpa/clients`, `monument`, `intake` |

### 1.2 API problems

| # | Problem |
|---|---|
| A1 | **Nothing publishes marks.** Marks/occurrences/evidences are admin-only; the platform's stated purpose (make marks public) has no endpoint. |
| A2 | **Layering leaks.** `MarkController` injects `MarkRepository` and serves `repository.findAll(pageable)`; some endpoints return entities. |
| A3 | **Scattered / duplicated DTOs** (`mark.dtos`, `review.dtos`, `bookmark.dto`). |
| A4 | **Inconsistent contracts.** `MessageResponseDto` vs `Map<String,String>` (global handler) vs raw strings; some endpoints unpaged. |
| A5 | **No visibility semantics** — no "published only" filter. |

---

## 2. Target domain model

### 2.1 Entities

**`Mark`** — the mark *design* (symbol type).
`{ id, title, description, status, publishedAt, coverMediaId }`
- `coverMediaId` is a plain media reference (removes the `exemplar → MarkEvidence` cycle).
- No `validationState`.

**`Monument`** — the *place*.
`{ id, name, description, protectionTitle, website, address, postalCode, location (Point,4326), status, publishedAt }`
- No `divisionCode`: administrative areas are **not** part of the model for now (see **D6**). The coordinates are the place data.

**`Occurrence`** — a mark design observed at a place.
`{ id, markId, monumentId (nullable), location (Point,4326) (nullable), period, status, publishedAt }`
- Carries its **own precise coordinates**; `Monument.location` is the coarse point (a centroid). They need not coincide — an occurrence can sit slightly outside/next to the monument point.
- **Located vs unlocated (a real category).** Coordinates are the normal, preferred case (field submission). An occurrence with neither coordinates nor monument is a legitimate **unlocated** entry — e.g. a mark identified from an old photo with no known place. Exposed/filterable as `located=true|false`.
- `(markId, monumentId)` uniqueness is a **partial** index (`WHERE deleted_at IS NULL AND monument_id IS NOT NULL`).

**`Evidence`** — a photo supporting an occurrence.
`{ id, occurrenceId, mediaId, submissionId (nullable), capturedAt, status }`
- Keeps the lineage back to the `Submission` it was approved from.

**`Submission`** — raw intake (unchanged concept), plus the review outcome is recorded in `ReviewDecision`.

**`ReviewDecision`** — the label (unchanged concept): `{ submissionId, outcome, markId, occurrenceId, reviewerId, decidedAt, comment }`.

### 2.2 Lifecycle

- One enum, `PublicationStatus { DRAFT, PUBLISHED, ARCHIVED }`, replaces the `EntityStatus` + `ValidationState` overlap on publishable entities.
- **`Mark` and `Occurrence` each carry their own `PublicationStatus`.** The **first** publish of a Mark cascade-publishes its existing Occurrences; Occurrences added *later* have an independent lifecycle (start `DRAFT`, published on their own).
- `publishedAt` set when moving to `PUBLISHED`.
- Soft delete (`deletedAt`) stays separate from lifecycle — deletion ≠ archival.
- One shared base (e.g. `AuditableEntity`) for all of the above, so audit/soft-delete is uniform.

### 2.3 Integrity rules

- **Foreign keys + soft delete together** (D5). They are complementary: FKs guarantee references point at physically existing rows; soft delete handles logical removal. Rules:
  - FK actions: **`ON DELETE RESTRICT`** — we soft-delete, so physical deletes are deliberate (you cannot remove a monument while occurrences still point at it).
  - FKs don't see soft deletes: a child may reference a *soft-deleted* parent row. **Every read filters `deleted_at IS NULL`**, so children of a removed parent are hidden by the query, not by the FK.
  - Uniqueness becomes **partial** (`WHERE deleted_at IS NULL`) so soft-deleted rows don't block re-creation.
  - A future hard-delete/purge must respect the FKs (children first, or a purge routine).
- Cross-module *Java* references remain as IDs (Modulith-clean); integrity is enforced by the DB, not by JPA relations across modules.
- FKs are added by the **migration step (C10)** — Hibernate cannot infer them from value-only columns.

### 2.4 Public visibility

- Public reads return only `status = PUBLISHED`.
- `ARCHIVED`/`DRAFT` are admin-only.

---

## 3. Target API

### 3.1 Public (read + submit)

- `POST /api/v1/public/submissions` — photo (req) + location (opt) + notes (opt) [exists]
- `GET  /api/v1/public/marks?q=&monument=&located=&page=` — paged catalogue (published only; `located` selects field vs unlocated) **[new]**
- `GET  /api/v1/public/marks/{id}` — mark + its occurrences + evidence media URLs **[new]**
- `GET  /api/v1/public/monuments`, `GET /api/v1/public/monuments/{id}`, `GET /api/v1/public/monuments/{id}/marks` **[partly exists]**
- `GET  /api/v1/public/media/{id}` [exists]

### 3.2 Admin

- `/api/v1/admin/marks` — CRUD + publish/archive
- `/api/v1/admin/occurrences` — CRUD (create with monument or coordinates)
- `/api/v1/admin/evidences`
- `/api/v1/admin/submissions` — queue
- `/api/v1/admin/review/{submissionId}/accept|accept-as-new|reject`

### 3.3 Conventions (applied everywhere)

- **Controllers never touch repositories** — services only.
- **One error DTO** (`{ code, message, details }`) from `GlobalExceptionHandler`.
- **One paging shape** (`Page<T>`) — no unpaged list endpoints.
- **DTOs** live next to the resource that owns them; no cross-module DTO leakage.
- Media returned by stable URL (`/api/v1/public/media/{id}`), not raw file paths.

---

## 4. Decisions (shape §2–§3)

All resolved:

| # | Decision | Resolution |
|---|---|---|
| D1 | Occurrences | **Keep.** `Mark` = design; `Occurrence` = design at a place; `Evidence` = photos. |
| D2 | Place | `Occurrence` has its **own coordinates** (preferred, field case); monument link optional; **unlocated** (no coordinates, no monument) is a valid category (a mark identified from a photo with unknown place). |
| D3 | Lifecycle | **Single `PublicationStatus`** (DRAFT/PUBLISHED/ARCHIVED) + `publishedAt`; soft delete separate. |
| D4 | Publishing | `Mark` **and** `Occurrence` each have a status. First Mark publish cascade-publishes current occurrences; later occurrences are independent. |
| D5 | Integrity | **FKs + soft delete together**: `ON DELETE RESTRICT`, partial unique indexes (`WHERE deleted_at IS NULL`), all reads filter `deleted_at IS NULL`. FKs land in C10. |
| D6 | Geo / divisions | **No home-grown geo service.** The abandoned `DivisionLookupClient`/`HttpDivisionLookupClient` (self-built `http://localhost:8081` geo API) and all `divisionCode` plumbing are **stripped**. If administrative areas are needed later, use an existing public PT open-data source — do not resurrect the self-built client. |

> A "designs only" collapse (D1) is off the table — occurrences stay.
> D6 removes the abandoned geo subsystem: `DivisionLookupClient`, `HttpDivisionLookupClient`, `DivisionRef`, `divisionCode` on `Monument`/`MarkEvidenceSubmission`, the `division` object in monument DTOs, the `divisionCode` admin filters, and the `/api/v1/public/monuments/division/{code}` endpoint. Coordinates (`Point`,4326) on monuments and occurrences are kept.

---

## 5. Work plan (commits)

Each commit: compiles, tests pass, `modules.verify()` green. `→ verify: mvn -B clean test`.

| Commit | Scope | Modules | Depends | Notes |
|---|---|---|---|---|
| **C1** | Add this design doc + link from `STABILIZATION.md` | docs | — | no code |
| **C2** | Introduce `PublicationStatus` + one `AuditableEntity` base; keep old fields temporarily | `shared` | D3 | additive, nothing removed yet |
| **C3** | Add DB foreign keys via migration (or noted for C10 if Flyway deferred) | `application` | D5 | integrity only |
| **C4** | Remodel `Mark`/`Occurrence`/`Evidence` to §2 (status, publishedAt, own coordinates + optional monument, unlocated category, evidence lineage); drop `ValidationState`/`exemplar`; **strip the abandoned geo lookup** (D6): remove the client/port/`DivisionRef`, the `divisionCode` plumbing in `monument`/`intake`, and the `/monuments/division/{code}` endpoint | `mark`, `monument`, `intake`, `shared` | D1–D4, D6 | schema change; Hibernate `ddl-auto=update` in dev |
| **C5** | Update `mark` repositories/specs/queries for the new model | `mark` | C4 | |
| **C6** | Public catalogue API: `GET /public/marks`, `/marks/{id}`, `/monuments/{id}/marks` with published-only filter | `mark`, `monument` | C4/C5 | the product surface |
| **C7** | `review`: create place-bearing occurrences from submission coordinates; publish mark on approval; record lineage | `review`, `intake` | C4 | closes M1/M4 |
| **C8** | `intake`: align submission ↔ evidence/media references with §2 | `intake` | C4 | |
| **C9** | Adapt dependents: `bookmark`, `support`, `processing` to new DTOs/entities | several | C4–C8 | keeps verify green |
| **C10** | Flyway baseline (`ddl-auto=validate`) once the model is settled | `application` | C4–C9 | schema becomes reproducible |
| **C11** | API conventions pass: single error DTO, paging everywhere, controllers off repositories, DTO tidy | all | C6–C9 | A2–A4 |
| **C12** | Docs: update `STABILIZATION.md` / module docs; remove stale model notes | docs | all | |

Sequencing notes:
- C2–C3 are additive foundations; they can land before the D-decisions if D3/D5 are accepted.
- C4 is the breaking commit; C5–C9 are the ripple.
- C10 is where `ddl-auto=update` stops being acceptable.

### Definition of done

- A public user can browse published marks (with evidence photos) and their places; anonymous submission still works.
- No cross-module repository access; `verify()` green.
- One lifecycle, one place model, one error/paging contract.
- `ddl-auto=validate` against a Flyway-managed schema.

### Non-goals (unchanged)

- ML/auto-categorisation (P3) — still deferred.
- Deployment/orchestration beyond running the API.
- Frontend adaptation (separate repos).

## 6. Risks

- **C4 is a schema-breaking change.** No production data yet, so acceptable; confirm before real submissions exist.
- **Cross-module FKs** trade module-table isolation for integrity — a deliberate call (D5).
- **Doing this before Flyway** means the baseline in C10 must capture the final shape; don't baseline twice.
