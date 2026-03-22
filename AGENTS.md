# AI Agent Guidelines

## Code Quality & Style
- Use modern Java 25 syntax and features where appropriate.
- Prioritize readable code over clever one-liners. Avoid unnecessary logic condensation.
- Write code that is easy to understand and maintain.
- Prefer parameterizing logic and reusing existing code over duplication.

## Comments & Documentation
- Comments must add real value and context, focusing on explaining the *why* behind complex logic or decisions.
- Only necessary comments should be included. Avoid redundant or excessive documentation.

## Architecture & Performance
- Aim for long-term reliability, scalability, and performance.
- Follow established design patterns and architectural principles suitable for Java projects.

## Workflow
- If a request implies a significant implementation or is ambiguous, ask for clarification before proceeding.
- Stick to the requested changes unless explicitly instructed otherwise.


Comment Rules:

- All comments must be written in English. Remove and replace any existing comments in other languages.
- Emojis and notations such as “NEW:”, “CHANGED:”, “EN:” are strictly prohibited.
- Comments must not be written in the first person. Focus on what the code does.
- Do not compare code versions or comment on previous errors or fixes.
- Do not modify or add content to .md files unless explicitly requested.
- Never place a Zero-Width Non-Breaking Space (BOM / ZWNBSP) at the beginning of a document.

Objective:

- Provide only necessary comments that add value and context.
- Documentation comments should explain the function's purpose within the file/component/class.
- Logical grouping should be achieved through clear class and method structure, and necessary comments where appropriate (e.g., constructor and fields, create, update, delete, get, get all, private helpers).
