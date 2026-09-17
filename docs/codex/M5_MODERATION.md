# Codex Task — M5 Moderation

Implement establishment moderation lifecycle.

Read `AGENTS.md`, `ESTABLISHMENT_LIFECYCLE.md`, `SECURITY.md`,
`DATABASE_DESIGN.md`, and ADR-004.

## Deliverables

- EstablishmentModerationEvent entity;
- ModerationAction enum;
- Flyway migration;
- moderation event repository;
- EstablishmentModerationService;
- admin moderation controller;
- pending/admin listing and detail;
- approve;
- reject with mandatory reason;
- owner resubmit;
- suspend;
- reactivate;
- moderation history;
- optimistic-lock conflict mapping;
- tests.

## Transaction requirement

Each moderation transition must atomically:

1. validate source state;
2. update current establishment state;
3. append moderation event;
4. commit.

## Allowed transitions

```text
PENDING_REVIEW -> APPROVED
PENDING_REVIEW -> REJECTED
REJECTED       -> PENDING_REVIEW
APPROVED       -> SUSPENDED
SUSPENDED      -> APPROVED
```

No other transition is valid in this milestone.

## Security

- approve/reject/suspend/reactivate require ADMIN;
- resubmit requires establishment owner;
- non-admin cannot call moderation endpoints.

## Acceptance criteria

Test at least:

- successful transitions;
- invalid transitions -> 409;
- non-admin forbidden;
- rejection reason requirement;
- moderation event persistence;
- public-state semantics remain consistent;
- optimistic locking conflict -> 409;
- `./mvnw test` passes.
