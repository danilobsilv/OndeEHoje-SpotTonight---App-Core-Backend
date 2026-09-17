# Codex Task — M1 Users and Authentication

Implement the users/authentication milestone only.

Read `AGENTS.md`, `AUTHENTICATION.md`, `DATABASE_DESIGN.md`, and ADR-003 first.

## Deliverables

- User entity;
- user roles;
- user status;
- RefreshToken entity;
- Flyway migrations;
- registration;
- login;
- JWT access tokens;
- refresh token generation/hashing/rotation;
- logout/revocation;
- `/api/v1/users/me`;
- Spring Security filter chain;
- JWT authentication filter;
- BCrypt password encoding;
- standardized error handling;
- integration/API tests.

## Rules

- public registration creates USER only;
- email normalized before persistence;
- roles are not client-controlled;
- refresh-token raw value never persisted;
- refresh tokens rotated after successful refresh;
- never log tokens/passwords.

## Acceptance criteria

At minimum test:

- successful registration;
- duplicate email conflict;
- successful login;
- invalid credentials;
- access authenticated endpoint;
- refresh succeeds;
- old rotated token cannot be reused;
- logout revokes refresh token;
- normal user cannot access admin routes;
- `./mvnw test` passes.
