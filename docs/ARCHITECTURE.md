# Architecture

> Code-level structure: modules, boundaries, conventions, security, configuration.
> Domain: [`OVERVIEW.md`](OVERVIEW.md). HTTP surface: [`API.md`](API.md).

## Shape

A **modular monolith** — Java 25, Spring Boot 4.0.4, Spring Modulith — with one Maven
module per capability, packaged by feature under `pt.estga.<module>`.

A module's packages are its own `entities`, `repositories`, `services`, `controllers`,
`mappers`, `config`, plus the named interfaces it exposes to other modules: `api`,
`dtos`, `events`, `enums`.

## Modules

| Module | Responsibility |
|---|---|
| `shared` | Cross-cutting bases and utilities: `AuditedEntity`, `PublishableEntity`, `SoftDeletable`, `PublicationStatus`, exceptions, `SpecBuilder`, rate limiting, `AppPrincipal`, `GeometryUtils`. Not a Modulith module. |
| `file` | Media storage on the local filesystem: staging, one web derivative, public media read API. |
| `monument` | Places: CRUD, public listing, polygon search. |
| `user` | Accounts, profiles, the single `UserRole`. |
| `mark` | Marks, occurrences, evidence, embeddings, and the public catalogue. |
| `intake` | Public photo submissions and the admin submission queue. |
| `review` | Moderation decisions (the labels). |
| `processing` | Vision detection, similarity search and suggestions (disabled by default). |
| `bookmark` | Bookmarks. |
| `support` | Contact requests and email/notifications. |
| `auth` | Security configuration, JWT, login/refresh, admin bootstrap. |
| `application` | Composition root: configuration, health, the Spring Boot entry point. |

Dependency direction flows toward `shared`; a module never reaches into another
module's internals.

## Boundaries (enforced)

`ModulithVerificationTest` runs `ApplicationModules.of(StonemarkApplication.class).verify()`
and fails the build on any violation.

- A module is reachable only through its exposed named interfaces, declared in its
  `package-info.java` via `allowedDependencies`.
- No cross-module repository access — a module owns its persistence and exposes
  services and DTOs instead.

## Conventions

- Controllers call services, never repositories; services return DTOs across the HTTP
  boundary.
- DTOs live next to the resource that owns them; a module's `dtos` package is part of
  its contract.
- One lifecycle (`PublicationStatus`), one soft-delete contract (`SoftDeletable`), one
  error shape (`GlobalExceptionHandler`), `Page<T>` for paged endpoints.
- Media is referenced by id and served from `/api/v1/public/media/{id}`.

## Security

- Password and JWT only, no external identity provider. The access token carries
  `user_id`, `preferred_username`, `token_version` and a single `role`.
- One role per user, ordered by a Spring `RoleHierarchy`
  (`ROLE_ADMIN > ROLE_MODERATOR > ROLE_REVIEWER > ROLE_USER`); an endpoint declares
  only its minimum role, for example `hasRole('MODERATOR')`.
- `/api/v1/public/**` and `/api/v1/auth/**` are open; other routes require
  authentication, and admin and moderation endpoints add a role requirement.
- The first administrator is created from `APPLICATION_BOOTSTRAP_ADMIN_USERNAME`,
  `_PASSWORD` and `_EMAIL` (disabled when unset).

## Async and AI

- Intake persists a submission in one transaction and publishes an event; the Vision
  listener runs after commit (`@TransactionalEventListener(AFTER_COMMIT)` with
  `@Async`).
- The Vision pipeline is gated by `processing.enabled` (default `false`); the Vision
  server is external. Internals:
  [`../processing/PROCESSING_MODULE.md`](../processing/PROCESSING_MODULE.md) and
  [`../processing/SCORING_CONTRACT.md`](../processing/SCORING_CONTRACT.md).

## Persistence

- PostgreSQL with PostGIS (`geometry(Point,4326)`) and pgvector (`vector(384)`
  embeddings).
- `ddl-auto=update` in development; Flyway bootstraps the required extensions.
- Soft delete uses `deletedAt`; reads filter deleted rows.

## Build and verify

- JDK 25 and the bundled Maven wrapper: `./mvnw -B clean test` (Windows:
  `.\mvnw.cmd -B clean test`).
- Tests are unit and `MockMvc`; they need no database. `ModulithVerificationTest`
  enforces boundaries and fails the build on violations.
- CI: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) builds and tests on
  JDK 25.

## Configuration switches

| Property | Default | Effect |
|---|---|---|
| `application.security.disabled` | `false` | `true` permits every request (local only) |
| `processing.enabled` | `false` | `true` enables the Vision pipeline |
| `application.bootstrap.admin.*` | unset | creates the first admin when set |
| `vision.server.url` | `http://localhost:8000` | external Vision server |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5440/stonemark` | database |
