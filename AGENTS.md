# AI Agent Guidelines

Code Quality & Style

- Use modern Java 25 syntax and features where appropriate.
- Prioritize readable, maintainable code over clever one-liners or overly condensed logic.
- Avoid duplication: reuse code and parameterize logic when possible.
- All imports must be declared at the top of the file; fully-qualified class names inline are discouraged.

Comments & Documentation

- Comments must add real value and context. Focus on why behind non-obvious logic or design decisions.
- Avoid trivial or redundant comments (for example, "increment i by 1").
- Only comment where it improves understanding or maintainability: constructors, fields, core methods, or non-trivial helpers.
- All comments must be in English. Remove any other-language comments.
- Do not use emojis, “NEW:”, “CHANGED:”, or personal pronouns in comments.

Architecture & Performance

- Code should follow long-term reliability, scalability, and performance principles.
- Apply suitable design patterns for Java backend projects.

Workflow & AI Use

- If a request is ambiguous, ask for clarification before proceeding.
- Stick to the requested changes unless instructed otherwise.
- Use AI suggestions for boilerplate or mechanical tasks only; keep core logic and architectural decisions under human review.

Objective

- Provide only necessary comments that improve maintainability.
- Ensure logical grouping through clear class/method structure.
- Avoid commenting trivial steps; comments should be production-grade.
- Enforce top-of-file imports and consistent style across files.
