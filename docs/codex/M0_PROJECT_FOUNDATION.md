# Codex Task — M0 Project Foundation

Implement only the project foundation.

Before coding, read the repository root `AGENTS.md` and all referenced architecture documentation.

## Goal

Bootstrap the Java backend infrastructure without implementing business features.

## Required deliverables

- Java 21+ Spring Boot project;
- Maven Wrapper;
- Spring MVC;
- Spring Data JPA;
- PostgreSQL driver;
- Flyway;
- Spring Security dependency, but no full auth implementation yet;
- Bean Validation;
- Spring Boot Actuator;
- OpenAPI/Swagger;
- Testcontainers PostgreSQL;
- Docker Compose service for local PostgreSQL;
- application profiles/configuration;
- `ddl-auto=validate`;
- `open-in-view=false`;
- base standardized error response;
- global exception handler foundation;
- request correlation ID filter/interceptor;
- health endpoint configuration;
- foundational tests proving application startup and PostgreSQL/Flyway integration.

## Do not implement

- User domain;
- authentication flows;
- Establishment domain;
- Category domain;
- recommendation logic;
- Redis;
- queues;
- PostGIS;
- frontend.

## Acceptance criteria

- `./mvnw test` passes.
- application starts against Docker PostgreSQL;
- Flyway executes successfully;
- no real credentials are committed;
- health endpoint works;
- Swagger/OpenAPI works in local development;
- code follows package-by-feature principles established in the docs.
