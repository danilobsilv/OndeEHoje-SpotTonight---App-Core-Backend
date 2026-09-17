# ADR-002 — PostgreSQL with Flyway

## Status

Accepted.

## Context

The application needs reliable relational persistence, schema constraints, transactional moderation workflows, and future support for more advanced location queries.

## Decision

Use PostgreSQL as the primary database.

Use Flyway as the only schema migration mechanism.

Configure Hibernate schema handling as validation rather than automatic production mutation.

## Consequences

Positive:

- strong relational/transactional semantics;
- production-like development;
- explicit schema history;
- future PostGIS path if justified.

Trade-offs:

- migrations require deliberate maintenance;
- tests should use real PostgreSQL semantics.

H2 is not the primary integration-test database.
