# ADR-005 — Separate Recommendation Service Later

## Status

Accepted.

## Context

Recommendation work is intentionally a separate learning/workstream in Python, while the core business backend is Java/Spring.

## Decision

The recommendation system will live in a separate Python/FastAPI repository.

Spring remains the public/business backend.

FastAPI will be internal and called by Spring.

FastAPI will not directly own or modify Spring's business database tables.

## Consequences

Positive:

- clean business/ML ownership;
- Python recommendation experimentation stays isolated;
- clients remain unaware of implementation split.

Trade-offs:

- future network calls introduce latency/failure modes;
- API versioning and service reliability will need explicit design.

No FastAPI integration is implemented until the recommendation milestone begins.
