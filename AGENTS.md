# AI Agent Guidelines

## Build & verification

- Java 25 is required. The JDK may not be on `PATH`; on Windows it is typically
  under `C:\Users\<user>\.jdks\`. Set `JAVA_HOME` and use the bundled Maven wrapper:

  ```powershell
  $env:JAVA_HOME="C:\Users\<user>\.jdks\ms-25.0.4.1"
  .\mvnw.cmd -B clean test
  ```

- **Do not build through Docker unless explicitly asked.** The local wrapper is the
  source of truth; Docker is slow and its container-writes can leave stale
  `target/` state on Windows that corrupts later builds.
- A change is green when the build reports `BUILD SUCCESS` and
  `ModulithVerificationTest` passes. Tests are unit / `MockMvc` and need **no
  database**.
- Do not edit files while a build is running — the build reads the working tree.

## Architecture & module rules

- This is a **modular monolith** (Spring Modulith). One Maven module per capability,
  packaged by feature under `pt.estga.<module>`.
- Cross-module access is only allowed through another module's **exposed named
  interfaces** (`api`, `dtos`, `events`, `enums`), declared in its
  `package-info.java` via `allowedDependencies`. `modules.verify()` fails the build
  on violations.
- **Controllers call services, never repositories.** Services return DTOs, not
  entities, across the HTTP boundary.
- Reuse the shared bases in `shared` (`AuditedEntity`, `PublishableEntity`,
  `SoftDeletable`, `PublicationStatus`) instead of inventing new lifecycle bases.
- Keep unused-but-plausible code; only fix what is genuinely broken.

## Code quality & style

- Use modern Java 25 syntax and features where appropriate.
- Prioritize readable, maintainable code over clever one-liners or overly condensed logic.
- Avoid duplication: reuse code and parameterize logic when possible.
- All imports must be declared at the top of the file; fully-qualified class names inline are discouraged.
- Code should follow long-term reliability, scalability, and performance principles.
- Apply suitable design patterns for Java backend projects.

## Comments & documentation

- Comments must add real value and context. Focus on why behind non-obvious logic or design decisions.
- Avoid trivial or redundant comments (for example, "increment i by 1").
- Only comment where it improves understanding or maintainability: constructors, fields, core methods, or non-trivial helpers.
- All comments must be in English. Remove any other-language comments.
- Do not use emojis, "NEW:", "CHANGED:", or personal pronouns in comments.
- Keep docs current: `README.md`, `docs/OVERVIEW.md` (purpose + domain),
  `docs/ARCHITECTURE.md` (structure), `docs/API.md` (endpoints). Describe the present
  and the intended future only — do not record history, decision logs, or change
  plans. Prefer updating an existing doc over adding a new one.

## Commit style

- Small, self-contained commits; each must build green.
- Messages are plain and descriptive, e.g. "Remove the abandoned geo lookup",
  "Simplify roles to a single ordered enum".
- **Never include plan/commit numbers or step ids in commit messages** — those are
  internal to the working session.
- Do not mix unrelated changes in one commit.

## Workflow & AI use

- If a request is ambiguous or destructive (deleting files, changing the model),
  ask for clarification before proceeding.
- Stick to the requested changes unless instructed otherwise.
- Use AI suggestions for boilerplate or mechanical tasks only; keep core logic and
  architectural decisions under human review.
