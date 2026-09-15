# AGENTS.md — Onde é Hoje Backend

This file contains repository-level instructions for Codex and other coding agents.

## Read first

Before implementing or modifying business behavior, read:

1. `docs/PRODUCT_SCOPE.md`
2. `docs/ARCHITECTURE.md`
3. `docs/DOMAIN_MODEL.md`
4. `docs/DATABASE_DESIGN.md`
5. `docs/API_GUIDELINES.md`
6. `docs/AUTHENTICATION.md`
7. `docs/ESTABLISHMENT_LIFECYCLE.md`
8. `docs/SECURITY.md`
9. `docs/TESTING_STRATEGY.md`
10. `docs/DEVELOPMENT.md`
11. `docs/ROADMAP.md`

Relevant ADRs under `docs/adr/` are authoritative for architectural decisions.

## Architecture constraints

- This repository is a **single Spring Boot modular monolith**.
- Use **Spring MVC**.
- Organize by feature, with MVC/layered structure inside each feature.
- Do not split modules into microservices unless explicitly instructed.
- Authentication is part of this application. Do not create an auth service.
- PostgreSQL is the system of record.
- Use Spring Data JPA/Hibernate for persistence.
- Use Flyway for every schema change.
- Hibernate `ddl-auto` must be `validate` outside ephemeral tests.
- The future recommendation system is a separate Python/FastAPI repository.
- Do not implement recommendation algorithms in this repository.
- The FastAPI recommendation service must not directly access Spring-owned database tables.

## Layering

Preferred flow:

```text
HTTP Request
   |
Controller
   |
Service
   |
Repository
   |
PostgreSQL
```

Rules:

- Controllers handle HTTP concerns only.
- Controllers must not call repositories directly.
- Business rules and state transitions belong in services.
- Repositories handle persistence concerns only.
- Transaction boundaries belong in service methods.
- JPA entities must not be used as request/response DTOs.
- Use explicit request/response DTOs.
- Prefer Java records for immutable DTOs when appropriate.

## Package structure

Use feature-first packaging.

Example:

```text
com.ondeehoje
├── auth/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   ├── dto/
│   └── security/
├── user/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   └── dto/
├── establishment/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   ├── dto/
│   └── mapper/
├── category/
└── shared/
```

Do not create global folders containing every controller/service/repository in the application.

## Domain invariants

These rules are mandatory.

### Users

- Public registration always creates a normal `USER`.
- A public request must never be able to assign `ADMIN`.
- Emails are normalized before persistence.
- Emails are unique case-insensitively at the application level; database constraints must also protect uniqueness.
- Passwords are never stored in plaintext.

### Establishments

- A newly created establishment always starts as `PENDING_REVIEW`.
- `ownerId` is derived from the authenticated principal, never accepted from client JSON.
- Only `APPROVED` establishments may appear in public discovery.
- Non-public establishments should normally appear as `404` through public endpoints.
- Owners may manage only their own establishments.
- Admin moderation actions must be auditable.
- State transitions must follow `docs/ESTABLISHMENT_LIFECYCLE.md`.
- Moderation changes must be transactional.
- Use optimistic locking on `Establishment`.

### Operating hours

- Multiple intervals per day are supported.
- Overnight intervals are supported.
- Operating status is distinct from establishment moderation status.
- Time calculations use the establishment's IANA timezone.
- Business logic should use an injectable `Clock`; avoid scattering `Instant.now()`.

## Authentication

Use:

- Spring Security;
- JWT access tokens;
- persistent opaque refresh tokens;
- refresh token hashing;
- refresh token rotation;
- HttpOnly/Secure cookie for refresh tokens when browser clients are used;
- BCrypt through Spring Security `PasswordEncoder` for V1.

Never log passwords, JWTs, or refresh tokens.

## API conventions

- Base path: `/api/v1`.
- JSON: camelCase.
- Domain IDs: UUID.
- Timestamps: ISO-8601 UTC.
- Use the standardized error schema defined in `docs/API_GUIDELINES.md`.
- Use Bean Validation on request DTOs.
- Use correct HTTP status codes.
- Pagination defaults must be bounded.
- Sorting fields must be allowlisted.

## Persistence

- PostgreSQL only.
- Use Flyway migrations in `src/main/resources/db/migration`.
- Never edit an applied/shared migration; add a new migration.
- Add database constraints for critical invariants.
- Default JPA relationships to lazy loading unless a use case proves otherwise.
- Avoid accidental N+1 queries.
- Do not add PostGIS until explicitly requested.

## Containerization and development workflows

- Backend containerization is a permanent M0 requirement.
- Maintain a multi-stage Dockerfile: Java 21 JDK and Maven Wrapper for the build;
  Java 21 JRE, application artifact, and healthcheck tooling only in the runtime.
- Run the backend container as a non-root user; never bake credentials into images.
- Maintain `.dockerignore` to exclude secrets, Git metadata, and host build artifacts.
- Compose must support both PostgreSQL alone for host-JVM development and the full
  backend + PostgreSQL stack. Keep `LOCAL_SETUP.md` tracked and document both modes.
- Preserve PostgreSQL's persistent volume and healthcheck. The backend must wait for
  database health and connect to `postgres:5432` through Compose networking.
- Publish the backend on local port 8080 and check `/actuator/health` for container health.
- Supply configuration through environment variables. Keep `.env` gitignored.
- Flyway runs during Spring startup; retain `ddl-auto=validate` and `open-in-view=false`.
- Validate container changes with Maven `test` and `verify`, Compose config/build/up,
  both services' health, Flyway success, and database connectivity over Docker networking.

## Testing requirements

No feature is complete without tests.

Expected coverage includes:

- happy path;
- invalid input;
- authorization failures;
- invalid state transitions;
- edge cases;
- persistence behavior for critical queries;
- API behavior for critical endpoints.

Use:

- JUnit;
- Mockito where useful;
- MockMvc for MVC/security/API behavior;
- Testcontainers with real PostgreSQL for persistence/integration tests.

Do not substitute H2 for PostgreSQL integration tests.

Before considering a task complete, run the repository's validation commands, typically:

```bash
./mvnw test
```

and, when configured:

```bash
./mvnw verify
```

## Logging and observability

- Use SLF4J.
- Support/request a correlation ID such as `X-Request-Id`.
- Include request ID in logs and response headers.
- Add Spring Boot Actuator.
- Do not expose sensitive actuator endpoints publicly by default.
- Never log secrets.

## Dependency policy

Do not introduce a new dependency unless it solves a concrete requirement in the task.

Do **not** independently add:

- Redis;
- Kafka;
- RabbitMQ;
- Elasticsearch/OpenSearch;
- PostGIS;
- Kubernetes;
- event sourcing;
- CQRS;
- service mesh;
- FastAPI;
- recommendation algorithms.

If a dependency or architectural change appears necessary, explain why before adding it.

## Change discipline

### Branch workflow

- Never implement changes or create commits directly on `main`.
- Before editing files, switch to an appropriate working branch; create one if needed.
- Keep all implementation, documentation, and configuration changes on working branches.
- Do not merge into `main` unless the user explicitly requests it.

When implementing a task:

1. Read the relevant docs/ADRs.
2. Inspect current implementation and tests.
3. Make the smallest coherent change.
4. Add/update Flyway migrations if the schema changes.
5. Add/update automated tests.
6. Run tests.
7. Report what changed, why, and any remaining risks.

Do not silently reinterpret product behavior.

If the documentation conflicts with code, call out the conflict instead of guessing.
