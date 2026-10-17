# StoneMark API — Behavioral De-engineering Plan

> Companion to `STABILIZATION.md`. `STABILIZATION.md` covers the structural work (P1, P2, P4, P5).
> This document covers the **behavioral/machinery** cuts that were deferred because each changes how the system runs and is therefore a product decision.

**Status:** §4 joins (`vision`→`processing`, `verification`→`user`, `notification`→`support`) **done**. §3 `chatbot` **removed**. §1 `file` reduced to **1 variant**. §2 (`processing` scaffolding), §5 (enforce verify) **pending decisions**.

## Principles

1. **Structure is done; this is behavior.** P4 already consolidated modules. Everything here changes runtime behavior, API surface, or operational machinery.
2. **Keep the product core, cut the scaffolding.** The content pipeline (intake → detect/embed → similarity → review) stays. The plating around it goes.
3. **Do not delete unused code just because it is unused** (owner guidance). Unused-but-plausible code is listed as "keep / decide", not removed.
4. **Each change is opt-in and reversible.** Nothing here is shipped until decided; each item states its behavior impact and the decision it needs.

## 1. `file` media/variant pipeline

**What exists.** Upload orchestrator + staging; `MediaProcessingService` (async, `@PostConstruct` webp check); `ImageVariantGenerator` (3 derivatives — `THUMBNAIL` 200², `PREVIEW` 1024², `OPTIMIZED` scale 1.0 — via thumbnailator → webp); `MediaVariant` entity + repo; `MediaVariantService` (cached lookup); `FileAsyncConfig` (`fileTaskExecutor`); `MediaUploadedEvent` → listener generates `OPTIMIZED`; `MediaApprovedEvent` → listener generates `THUMBNAIL`+`PREVIEW` (**never published — unreachable**); `MediaRecoveryJob` (stuck `PROCESSING` → `FAILED`); `TempFileCleanupJob`; `MediaController` exposes `/{id}/thumbnail|preview|optimized`.

**Why it was heavy.** Three derivatives, an event-driven async pipeline, a cache, a recovery job and two events — for what is, at the catalogue's scale, "show a web-friendly image".

**Resolved: D1 = one derivative (done).**
- `MediaVariantType` now has a single value, `OPTIMIZED`; `ImageVariantGenerator` produces only it.
- `MediaApprovedEvent` and its (unreachable) handler were removed.
- `MediaController` no longer exposes `/{id}/thumbnail` or `/{id}/preview`; only `/{id}/optimized` remains.
- `ImageVariantGeneratorTest` updated to the single variant.

**Still present (not part of D1 — flag for later):** the async event path (`MediaUploadedEvent` → listener → `MediaProcessingService`), `FileAsyncConfig`, the `MediaVariantService` cache, and `MediaRecoveryJob`. Cutting those is a further, optional step.

**Behavior impact.** `GET /{id}/thumbnail` and `/{id}/preview` are gone; the UI must use `/{id}/optimized`. This is a **public API change** (frontends are out of scope here).

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

**Also removed** (existed only for the chatbot channel): `user.verification.*` (`ChatbotVerificationService`, `ActionCode`, `ActionCodeRepository`, `ActionCodeType`, `ChatbotVerificationRequestDto`, `AccountVerificationController`), `User.telegramChatId` and `UserRepository.findByTelegramChatId`, `intake`'s `submitStaged`, the `action-code` config block, and the chatbot rate-limit path.

**Behavior impact.** Web submission is unaffected and is the only channel. Recovering the code is a `git revert` away.

**Note on WhatsApp.** Telegram was the development stand-in for the stakeholder's WhatsApp idea. WhatsApp is a **separate integration** (Meta WhatsApp Business Cloud API: business account, dedicated number, webhook verification, approved templates) — not a config switch. The flow/handlers are reusable groundwork; a WhatsApp adapter would be a new, separate piece of work.

## 4. Module joins (structural, zero behavior change)

**Done.**
- `vision` → `processing` (package `pt.estga.processing.vision`).
- `verification` → `user` (package `pt.estga.user.verification`) — the folded classes were later **deleted** along with the chatbot removal.
- `notification` → `support` (package `pt.estga.support.notification`; mail templates + `application-notification.yml` moved too).

**Behavior impact:** none. Verified by the build.

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

- **D1 — `file` derivatives:** ✅ **1** (`OPTIMIZED`). `THUMBNAIL`/`PREVIEW` and the `/thumbnail`/`/preview` endpoints removed.
- **D2 — `chatbot`:** ✅ **removed** (module deleted; Telegram was a dev stand-in only).
- **D3 — `notification` home:** ✅ **folded into `support`**.
- **D4 — enforce `modules.verify()`:** now / after boundary cleanup (recommend after).

## Explicitly out of scope

- Deleting unused-but-plausible code (`MarkOccurrenceSuggestion`, unreachable `MediaApprovedEvent`, extra query overloads) — kept per owner guidance.
- Deployment (Docker/compose), Flyway migrations, the public read API — deferred in `STABILIZATION.md`.
- P3 (train + automate) — the separate ML plan in `marks-processing/PLAN.md`.
