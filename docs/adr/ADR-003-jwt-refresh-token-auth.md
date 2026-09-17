# ADR-003 — JWT Access Tokens with Persistent Rotating Refresh Tokens

## Status

Accepted.

## Context

The backend needs stateless access-token verification while preserving revocation/logout capability for long-lived sessions.

## Decision

Use:

- short-lived JWT access tokens;
- long-lived opaque refresh tokens;
- hash refresh tokens before persistence;
- rotate refresh tokens on refresh;
- revoke refresh tokens on logout.

Authentication remains inside the Spring application.

## Consequences

Positive:

- access requests do not require session lookup;
- refresh sessions can be revoked;
- token theft impact is reduced through rotation.

Trade-offs:

- refresh-token lifecycle adds complexity;
- access JWTs remain valid until expiration unless additional revocation infrastructure is introduced.

That trade-off is acceptable for V1.
