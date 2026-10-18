# StoneMark API

> Preserving masons' marks — a modular REST backend for collecting, moderating and publishing masonic marks.

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.4-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-PostGIS%20%2B%20pgvector-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

## What it is

Masons' marks are symbols carved into stone by stonemasons to identify their work,
crew or provenance. They survive on monuments (churches, walls, bridges) but are
poorly catalogued. StoneMark is the backend that builds that catalogue:

- **Collect** — anyone can submit a photo (optionally with a location); no account needed.
- **Curate** — a moderator decides what each photo is, producing clean data and the
  training labels for later automation.
- **Publish** — the curated catalogue of marks and their locations is served publicly.
- **Automate (later)** — matching a finding to an existing mark, and detection, once
  enough labelled data exists.

The domain model and product direction are in **[docs/OVERVIEW.md](docs/OVERVIEW.md)**.

## Where the project is

| Area | State |
|---|---|
| Public photo intake (atomic, idempotent) | working |
| Manual moderation / labelling | working |
| Marks, occurrences and monuments catalogue with a publish lifecycle | working |
| Public read API for published marks | working |
| Password + JWT auth, single ordered role | working |
| Local filesystem media, one web derivative | working |
| Vision / AI pipeline | **off by default** — classic shape descriptors, no trained model |
| Flyway migrations + DB foreign keys, deployment, ML training | deferred |

The HTTP surface is in **[docs/API.md](docs/API.md)**.

## Modules (12)

One Maven module per capability, packaged by feature under `pt.estga.<module>`:

| Module | Responsibility |
|---|---|
| `shared` | Cross-cutting bases: entities (`AuditedEntity`, `SoftDeletableEntity`, `PublishableEntity`), DTOs, exceptions, specs, rate limiting, security helpers. Not a Modulith module. |
| `file` | Media storage (local filesystem), staging, one web derivative, public media read API. |
| `monument` | Places (monuments): CRUD, public listing, polygon search. |
| `user` | Accounts, profiles, the single `UserRole`. |
| `mark` | Marks, occurrences, evidence, embeddings, and the public catalogue. |
| `intake` | Public photo submissions and the admin submission queue. |
| `review` | Moderation decisions (the labels). |
| `processing` | Shape-descriptor similarity search and suggestions (disabled by default). |
| `bookmark` | User bookmarks. |
| `support` | Contact requests and email/notifications. |
| `auth` | Security configuration, JWT, login/refresh, admin bootstrap. |
| `application` | Composition root: configuration, health, the Spring Boot entry point. |

Boundaries are enforced by Modulith — see **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

## Quick start

Prerequisites: **JDK 25**, the bundled **Maven Wrapper** (`./mvnw`, `.\mvnw.cmd`), and
**Docker** for the database.

```powershell
# 1. Create .env (see .env.example) with DB_USERNAME and DB_PASSWORD.

# 2. Start PostgreSQL with PostGIS + pgvector (host port 5440).
docker-compose up -d database

# 3. Build and test.
.\mvnw.cmd -B clean test

# 4. Run the API (or run pt.estga.StonemarkApplication from your IDE).
cd application
..\mvnw.cmd spring-boot:run
```

Swagger UI: <http://localhost:8080/swagger-ui/index.html>

To explore without a trained model, the AI pipeline stays off
(`processing.enabled=false`). To run without auth locally, set
`application.security.disabled=true`.

## Documentation

| Document | Contents |
|---|---|
| [docs/OVERVIEW.md](docs/OVERVIEW.md) | Purpose, domain model, direction. |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Modules, boundaries, conventions, security, configuration, build. |
| [docs/API.md](docs/API.md) | HTTP endpoints. |
| [processing/PROCESSING_MODULE.md](processing/PROCESSING_MODULE.md) | Vision pipeline internals. |
| [processing/SCORING_CONTRACT.md](processing/SCORING_CONTRACT.md) | Similarity scoring contract. |
| [AGENTS.md](AGENTS.md) | Conventions for (AI) contributors. |

## License

MIT — see [`licence.txt`](licence.txt).
