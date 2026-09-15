# ADR-004 — Moderated Establishment Registration

## Status

Accepted.

## Context

Establishments may self-register, but user-submitted businesses must not become publicly visible without verification.

## Decision

All new establishments start as `PENDING_REVIEW`.

Only admins may approve/reject.

Use explicit state transitions and persistent moderation events.

Only `APPROVED` establishments appear publicly.

## Consequences

Positive:

- moderation cannot be bypassed through normal API input;
- decisions are auditable;
- public discovery is protected from unreviewed submissions.

Trade-offs:

- moderation creates an operational workflow;
- sensitive edits to approved establishments require future workflow design.

V1 restricts sensitive approved-establishment changes rather than implementing full change-request versioning.
