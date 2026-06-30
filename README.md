# StoneMark API

> Preserving history through technology — a modular REST backend for documenting and analysing masons' marks.

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.4-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

## About

StoneMark API is the backend service for the StoneMark project, dedicated to the digital preservation of masons' marks — symbols carved by stonemasons used to identify work, provenance, or crew.

This service provides:
- Storage and retrieval for marks and associated metadata (images, location, estimated date, notes).
- Asset storage using MinIO (S3-compatible).
- Search and geospatial tooling for monuments and territories.
- Integration points for computer vision-based detection (external Vision Server) and chatbot interfaces.

## Key features

- Core CRUD for marks, monuments and related domain objects.
- Advanced search and filtering across symbols and locations.
- Digital asset management with MinIO.
- Computer-vision integration hooks (Vision Server is external to this repository).
- Community workflows: submissions, verification and reporting.
- Bot integrations (Telegram / WhatsApp) via the `chatbot` module.

## Technology

- Language: Java 25
- Framework: Spring Boot 4.0.4
  - Spring Web (REST)
  - Spring Data JPA (Hibernate)
- Database: PostgreSQL 18 (image based on PostGIS 3.6 — see `postgres/Dockerfile`)
  - pgvector is installed in the DB image for vector search support
- Storage: MinIO (S3 API)
- API docs: SpringDoc OpenAPI (swagger UI available)
- Build: Maven (mvn / Maven Wrapper)
- Containers: Docker & Docker Compose

## Prerequisites

- Java 25 JDK
- Maven 3.8+ (or use the bundled Maven Wrapper: `mvnw`, `mvnw.cmd`)
- Docker & Docker Compose
- Git

## Quick start (recommended: Docker)

1. Create a `.env` file in the repository root with required values (examples below).

Example `.env` (store secrets securely in real deployments):

```properties
# Database
DB_USERNAME=postgres
DB_PASSWORD=changeme

# MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin

# Optional: bots / notifications
TELEGRAM_BOT_TOKEN=
WHATSAPP_BOT_TOKEN=
MAIL_ADDRESS=
MAIL_PASSWORD=
```

2. Start infrastructure services (PowerShell):

```powershell
# From repository root
docker-compose up --build database minio 
```

Notes on service ports (as configured in `docker-compose.yml`):
- Application (when run locally): 8080
- MinIO API: http://localhost:9000
- MinIO Console: http://localhost:9001
- PostgreSQL (container): 5432 � mapped to host 5440

3. Build and run the API (PowerShell):

```powershell
# Build all modules
.\n+\mvnw.cmd -DskipTests clean package

# Run the boot module locally (or use your IDE to run pt.estga.StonemarkApplication)
cd boot
..\mvnw.cmd spring-boot:run
```

If the `app` service is enabled in `docker-compose.yml` (it is commented out by default), it will bind to port 8080.

## Running without Docker (developer flow)

1. Ensure a PostgreSQL instance with PostGIS and pgvector is available and reachable; update environment variables or `application.yml` accordingly.
2. Build and run the `boot` module the same way as above.

## API documentation

When the application is running, the Swagger UI is available at:

http://localhost:8080/swagger-ui/index.html

The application sets the OpenAPI base URL using `application.base-url` (see `boot/src/main/resources`).

## Notes about detection and vision

The Vision Server used for automated detection is external and not provided in this repository or the default Docker Compose. Integration points exist in the `detection` module and environment variables reference a `VISION_SERVER_URL` if set. If detection is required, provide or run the Vision Server separately and set the `VISION_SERVER_URL` environment variable.

## Database image details

The `postgres` folder contains a Dockerfile based on `postgis/postgis:18-3.6` and installs `pgvector` (v0.8.1) so the DB image supports vector search. The compose mapping exposes the DB on host port 5440.

## Project modules

This repository uses a modular monolith layout. Modules defined in the root POM include:

- `boot` — application entry and composition; wires modules together and hosts the Spring Boot application.
- `mark` — domain logic and persistence for masons' marks and related entities.
- `vision` — adapters and services for computer-vision detection (integration hooks to external Vision Server).
- `file` — file storage and retrieval utilities; MinIO client integration and upload handling.
- `user` — account management, authentication and profile operations.
- `shared` — shared DTOs, common utilities, exceptions and cross-module helpers.
- `intake` — user-submitted marks and lifecycle management for suggested content.
- `support` — operational helpers and supporting services used across modules (email, scheduling, utilities).
- `chatbot` — bot integrations for Telegram/WhatsApp and conversational handlers.
- `verification` — admin verification workflows and moderation tooling for submissions.
- `report` — reporting and export features (analytics, generated reports).
- `territory` — geospatial models and territory/administrative division management.
- `decision` — business rules and decision-making workflows (automated or manual rule engines).
- `shared-web` — web utilities, OpenAPI configuration and Swagger integration.
- `bookmark` — user bookmarking and favorites management.
- `content-import` — importers/parsers for bulk content ingestion and ETL tasks.
- `monument` — domain and CRUD for monuments and physical locations.
- `notification` — notification dispatching (email) and template management.

## Development notes

- Swagger/OpenAPI is provided by SpringDoc (see `shared-web/pom.xml`).
- The repository provides a Maven Wrapper (`mvnw`, `mvnw.cmd`) for reproducible builds on CI and contributors' machines.

## Contributing

Contributions are welcome. Suggested workflow:

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature`.
3. Commit changes with clear messages.
4. Push and open a Pull Request.

Please follow existing code style and module boundaries.

## License

This project is licensed under the MIT License — see the `LICENSE` file for details.
