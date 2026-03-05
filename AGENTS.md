# AI Agent Guidelines

## Code Quality & Style
- **Java 21**: Utilize modern Java 21 syntax and features where appropriate.
- **Readability**: Prioritize "boring", readable code over clever one-liners. Do not condense logic unnecessarily.
- **Maintainability**: Write code that is easy to understand and maintain for future developers.
- **Reusability**: Prefer parameterizing logic and reusing existing code over duplication.

## Comments & Documentation
- **Value**: Comments must add real value. Avoid redundant comments that simply state what the code is doing.
- **Context**: Focus on explaining the *why* behind complex logic or decisions, rather than the *how*.

## Architecture & Performance
- **Goals**: All changes should aim for long-term reliability, scalability, and performance.
- **Best Practices**: Adhere to established design patterns and architectural principles suitable for the project.

## Workflow
- **Clarification**: If a request implies a significant implementation or is ambiguous, ask for clarification before proceeding.
- **Scope**: Stick to the requested changes unless explicitly instructed otherwise.


Comment Rules:

All comments must be written in English. The use of any other language is strictly prohibited. If comments already exist in another language, they must be removed and replaced with new ones written in English.

The use of emojis is strictly prohibited.

The use of notations such as examples like “NEW:”, “CHANGED:”, “EN:”, among others, is strictly prohibited.

Comments must never be written in the first person, such as using “I” or “We”. The focus must always be on what the code does, not on the intention behind writing the code.

Code versions must not be compared. Do not comment on what existed before compared to now, what error was fixed, etc. Avoid any “before vs after” comparison. The objective of comments is always to explain the final version to others.

Do not modify or add content to .md files unless it is explicitly requested.

Never place a Zero-Width Non-Breaking Space (BOM / ZWNBSP) at the beginning of a document.

Objective:

Provide exhaustive and complete comments.

All functions must include documentation comments explaining what the function does and its purpose within the file/component/class, etc.

Larger or more complex functions, or those that may be difficult to understand, must be reinforced with multiple inline comments to help users easily follow the flow and understand how it works.

The document should be complemented with #regions. Note that it is not necessary to create a region for each individual function; regions may be grouped logically. Examples (depending on the file’s context) include: Constructor and attributes/fields, create, update, delete, get, get all, private helpers, etc.