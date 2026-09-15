# Local Development

## Preferred local workflow

Run PostgreSQL through Docker, and Spring Boot directly from the developer machine.

```text
Spring Boot (local JVM)
      |
      v
PostgreSQL (Docker)
```

Example:

```bash
docker compose up -d --wait postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

This preserves easy debugging while keeping PostgreSQL reproducible.

## Full container workflow

A multi-stage backend Dockerfile and Docker Compose configuration are required as
part of M0. Both the host-JVM workflow and the fully containerized backend +
PostgreSQL workflow must remain supported.

```bash
docker compose build
docker compose up -d --wait
```

The backend waits for healthy PostgreSQL, connects to `postgres:5432` over the
Compose network, and exposes port 8080 on localhost. Flyway runs during Spring
startup. Both services have healthchecks and PostgreSQL uses a persistent volume.

See [LOCAL_SETUP.md](../LOCAL_SETUP.md) for environment configuration, prerequisites,
both workflows, and validation commands. Compose reads `.env`; Spring Boot on the
host requires the corresponding environment variables to be exported.

## Configuration

Recommended profiles:

```text
application.yml
application-local.yml
application-test.yml
application-prod.yml
```

Sensitive values come from environment variables.

## Persistence configuration

Use:

```text
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
```

Flyway is authoritative for schema creation and migration.

## Java

Target:

```text
Java 21+
```

Use Maven Wrapper.

Prefer explicit Java code over Lombok in the initial educational codebase.

## API documentation

Use OpenAPI/Swagger for generated interactive API docs.

Markdown documentation remains authoritative for product semantics and architectural decisions.

## Logging

Use SLF4J.

Support `X-Request-Id`.

If request ID is absent, generate one.

Include request ID in response and logs.

## Health

Use Spring Boot Actuator.

At minimum expose an appropriate health endpoint for local/container health checks.
