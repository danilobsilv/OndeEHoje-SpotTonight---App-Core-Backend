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
docker compose up -d postgres
./mvnw spring-boot:run
```

This preserves easy debugging while keeping PostgreSQL reproducible.

## Full container workflow

A Dockerfile and Docker Compose configuration may also run the complete backend as containers.

This is useful for deployment parity and CI.

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
