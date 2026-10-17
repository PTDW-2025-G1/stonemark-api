# StoneMark API — Behavioral De-engineering Plan

> Companion to `STABILIZATION.md`. `STABILIZATION.md` covers the structural work (P1, P2, P4, P5).
> This document covers the **behavioral/machinery** cuts that were deferred because each changes how the system runs and is therefore a product decision.

**Status:** §4 joins (`vision`→`processing`, `verification`→`user`) **done**. §3 `chatbot` **removed**. §1 (`file` variants), §2 (`processing` scaffolding), §5 (enforce verify) **pending decisions**.

## Principles

1. **Structure is done; this is behavior.** P4 already consolidated modules. Everything here changes runtime behavior, API surface, or operational machinery.
2. **Keep the product core, cut the scaffolding.** The content pipeline (intake → detect/embed → similarity → review) stays. The plating around it goes.
3. **Do not delete unused code just because it is unused** (owner guidance). Unused-but-plausible code is listed as "keep / decide", not removed.
4. **Each change is opt-in and reversible.** Nothing here is shipped until decided; each item states its behavior impact and the decision it needs.

## 1. `file` media/variant pipeline

**What exists.** Upload orchestrator + staging; `MediaProcessingService` (async, `@PostConstruct` webp check); `ImageVariantGenerator` (3 derivatives — `THUMBNAIL` 200², `PREVIEW` 1024², `OPTIMIZED` scale 1.0 — via thumbnailator → webp); `MediaVariant` entity + repo; `MediaVariantService` (cached lookup); `FileAsyncConfig` (`fileTaskExecutor`); `MediaUploadedEvent` → listener generates `OPTIMIZED`; `MediaApprovedEvent` → listener generates `THUMBNAIL`+`PREVIEW` (**never published — unreachable**); `MediaRecoveryJob` (stuck `PROCESSING` → `FAILED`); `TempFileCleanupJob`; `MediaController` exposes `/{id}/thumbnail|preview|optimized`.

**Why it is heavy.** Three derivatives, an event-driven async pipeline, a cache, a recovery job and two events — for what is, at the catalogue's scale, "show a web-friendly image".

**Options.**
- **A — Keep all three derivatives** (status quo).
- **B — One derivative.** Generate a single web-optimized derivative (e.g. `OPTIMIZED`, ~1600px webp) on upload, synchronously; serve the original as fallback; drop `THUMBNAIL`/`PREVIEW`, `MediaApprovedEvent`, the async path and the recovery job.
- **C — No derivatives.** Store and serve the original only; let the client/CDN resize.

**Recommendation: B.** One derivative covers both grid thumbnails and detail views; it removes an event, a job and a cache.

**Behavior impact.** `GET /{id}/thumbnail` and `/{id}/preview` disappear (or 404); the UI must use one image URL. This is a **public API change**.

**Risk.** `MediaControllerTest`, `ImageVariantGeneratorTest`, and `stonemark-react`/`react-admin-panel` (out of scope here) assume the three endpoints.

**Decision needed (D1):** how many sizes does the public UI actually need?

## 2. `processing` operational scaffolding

**What exists.** Two schedulers that both mutate processing records — `ProcessingRetryScheduler` (exponential backoff, orphan collection, `Thread.sleep` batching) and `ProcessingStuckReaper` (reset stuck `PROCESSING`); a per-JVM `Semaphore` for Vision concurrency; `ProcessingAsyncConfig` (thread pool + gauges); the unused `MarkOccurrenceSuggestion` entity; unbound scoring config keys in `application-processing.yaml`.

**Why it is heavy.** Two competing recovery mechanisms; JVM-local concurrency that isn't cluster-safe; and all of it predates a pipeline that actually runs (the pipeline is **disabled** in V1).

**Options.**
- **A — Keep** as-is.
- **B — Consolidate:** one recovery job (retry `PENDING`/`FAILED` past a threshold; reset `PROCESSING` past a longer threshold), a bounded executor sized by config instead of a hand-rolled semaphore, and prune the unbound config keys.
- **C — Defer** until the pipeline is switched on.

**Recommendation: C now, B at P3.** Do not tune machinery for a subsystem that is off. Revisit when a trained model makes `processing.enabled=true` meaningful.

**Behavior impact.** None while the pipeline is disabled.

**Keep regardless (owner guidance):** `MarkOccurrenceSuggestion` and the extra API overloads — unused, but plausibly planned. Not removed.

## 3. `chatbot`

**What exists.** The largest module (48 files): a conversational engine (`BotEngine`, `ConversationDispatcher`) with per-step context/state classes, a custom UI model (`models/ui/*`), i18n (`UiTextService` + `messages.properties`), an emoji provider, a Telegram adapter/bot/webhook, a health indicator, a notification service, and submission + verification handlers.

**Why it is heavy.** A general conversational-UI framework for what is a **secondary ingestion channel** (photo + location + notes → submission).

**Options.** ~~keep / simplify / park~~
- **A — Keep** the full engine.
- **B — Simplify to a linear ingestion flow** (photo → optional location → optional notes → submit), dropping the general engine, the UI component model and i18n (single locale).
- **C — Park the module** (leave the code, disable the channel).

**Resolved: removed.** The module was deleted (recoverable from git history). It was never used in production — Telegram was only ever a development stand-in for the stakeholder's WhatsApp idea — and it was a **launch blocker**: the app would not boot without `TELEGRAM_BOT_TOKEN` (`TelegramBotConfig.@PostConstruct` throws, the bot sets its webhook at startup, readiness health depended on Telegram). Web submission remains the only channel and is unaffected.

**Now orphaned (kept, per owner guidance — flag for a later decision):** `user.verification.*` (`ChatbotVerificationService`, `ActionCode`, `AccountVerificationController`) and `intake`'s `submitStaged` existed only for the chatbot channel.

**Behavior impact.** Web submission is unaffected and is the only channel. Recovering the code is a `git revert` away.

**Note on WhatsApp.** Telegram was the development stand-in for the stakeholder's WhatsApp idea. WhatsApp is a **separate integration** (Meta WhatsApp Business Cloud API: business account, dedicated number, webhook verification, approved templates) — not a config switch. The flow/handlers are reusable groundwork; a WhatsApp adapter would be a new, separate piece of work.

## 4. Remaining module joins (structural, zero behavior change)

Ideally done in P4; listed here because they touch small modules whose home is a judgement call.

| Join | Rationale | Consumers |
|---|---|---|
| `vision` → `processing` | `VisionClient` is used only by the pipeline | `processing` |
| `verification` → `user` | it only links a chatbot identity to an account | `chatbot` |
| `notification` → `support` | email is a support concern | `support`, `application` (`EmailAdminController`) |

**Recommendation:** do `vision`→`processing` and `verification`→`user`; decide `notification` (D3).

**Behavior impact:** none. **Risk:** low. Verified by the build, like P4.

## 5. Enforce `modules.verify()`

**What exists.** `ModulithVerificationTest` prints violations but never fails the build. The current set (as of P4) still lists many cross-module references — notably `review`→`processing`/`mark`, `chatbot`→`intake`/`file`/`user`, `verification`→`user`, `processing`→`intake`. Several are declared `allowedDependencies` yet still reported, a residue of the old `*-api` split (partly resolved by P4).

**Why it matters.** Without enforcement the boundaries erode silently — the original problem this whole effort started from.

**Options.**
- **A — Keep informational.**
- **B — Make it fail**, after routing cross-module access through `.api`/`@NamedInterface` and correcting `allowedDependencies`.

**Recommendation: B, as a dedicated follow-up.** First re-run `verify()` now that P4 landed and triage the remaining set — some entries may already be resolved or misreported.

**Behavior impact:** none; it adds a guardrail.

## Sequencing

| Order | Work | Behavior change | Gated on |
|---|---|---|---|
| 1 | §4 joins (`vision`, `verification`) | none | nothing — can do now |
| 2 | §5 verify triage (diagnostic only) | none | nothing |
| 3 | §3 `chatbot` | yes | **D2** |
| 4 | §1 `file` variants | yes (API) | **D1** |
| 5 | §5 enforce verify | none | §4 + boundary fixes |
| 6 | §2 `processing` scaffolding | none while off | **P3** |

## Open decisions

- **D1 — `file` derivatives:** 3 / 1 / none (recommend 1).
- **D2 — `chatbot`:** ✅ **removed** (module deleted; Telegram was a dev stand-in only).
- **D3 — `notification` home:** keep standalone / fold into `support` (recommend fold).
- **D4 — enforce `modules.verify()`:** now / after boundary cleanup (recommend after).

## Explicitly out of scope

- Deleting unused-but-plausible code (`MarkOccurrenceSuggestion`, unreachable `MediaApprovedEvent`, extra query overloads) — kept per owner guidance.
- Deployment (Docker/compose), Flyway migrations, the public read API — deferred in `STABILIZATION.md`.
- P3 (train + automate) — the separate ML plan in `marks-processing/PLAN.md`.
