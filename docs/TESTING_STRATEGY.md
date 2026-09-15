# Testing Strategy

The project treats tests as part of feature completion.

## Unit tests

Use for business logic that does not require Spring/database infrastructure.

Examples:

- establishment state transitions;
- operating-hours calculations;
- ownership/business validation;
- token-related logic;
- utility validation.

Use JUnit.

Use Mockito only when mocks simplify isolation; do not mock everything automatically.

## MVC tests

Use MockMvc for:

- request validation;
- status codes;
- JSON serialization;
- authentication;
- authorization;
- standardized errors.

## Repository/integration tests

Use Testcontainers with real PostgreSQL.

Do not use H2 as the primary persistence compatibility test.

Test:

- database constraints;
- repository queries;
- transaction behavior where important;
- Flyway compatibility.

## End-to-end backend integration flows

Critical examples:

```text
register
  -> login
  -> create establishment
  -> admin approves
  -> establishment becomes publicly discoverable
```

Also:

```text
create
  -> reject
  -> owner edits
  -> resubmit
  -> approve
```

## Moderation tests

At minimum:

- admin can approve pending establishment;
- non-admin cannot approve;
- invalid source state produces conflict;
- rejection requires reason;
- moderation event is created;
- public visibility changes correctly;
- suspension removes public visibility;
- reactivation restores it;
- stale optimistic-lock update results in conflict.

## Operating-hours tests

Cover:

- normal same-day interval;
- multiple intervals in same day;
- exact opening boundary;
- exact closing boundary;
- overnight interval;
- previous-day overnight lookup;
- closed day;
- timezone conversion;
- fixed Clock behavior.

## Validation command

Typical:

```bash
./mvnw test
```

When build verification is configured:

```bash
./mvnw verify
```

Codex must run validation before considering a task complete.
