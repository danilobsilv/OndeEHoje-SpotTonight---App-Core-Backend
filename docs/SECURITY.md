# Security Guidelines

## Principles

- least privilege;
- deny by default where practical;
- server-controlled ownership/status;
- explicit authorization;
- no secret logging;
- no secrets committed to source control;
- input validation at multiple layers.

## Authentication

See `AUTHENTICATION.md`.

## Authorization

- admin endpoints require `ADMIN`;
- owner operations validate establishment ownership in the service layer;
- public APIs expose approved establishments only;
- clients cannot assign owner IDs or moderation status.

## Secrets

Configuration is supplied through environment variables or a future secret manager.

Examples:

```text
DB_USERNAME
DB_PASSWORD
JWT_SECRET
ACCESS_TOKEN_TTL
REFRESH_TOKEN_TTL
CORS_ALLOWED_ORIGINS
```

Never commit real credentials.

## Logging

Never log:

- passwords;
- raw JWTs;
- refresh tokens;
- database passwords;
- secret values.

Authentication failures may be logged with safe metadata.

## CORS

CORS must be restricted to known frontend origins in production.

Do not deploy production with wildcard origins together with credentialed browser requests.

## Database

- PostgreSQL is not publicly exposed in production;
- database credentials use least privilege;
- critical constraints exist in PostgreSQL as well as Java;
- schema evolution is Flyway-controlled.

## Public information disclosure

Avoid revealing private resource existence.

Example:

A user requesting another owner's private establishment should generally receive `404`.

## Actuator

Expose only required health/observability endpoints.

Do not publish sensitive actuator endpoints to the Internet by default.

## Dependency changes

New security-related dependencies must have a concrete reason and be reviewed before introduction.
