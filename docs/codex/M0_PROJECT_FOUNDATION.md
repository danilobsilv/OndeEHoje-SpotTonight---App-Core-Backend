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
- production-oriented multi-stage backend Dockerfile using Java 21 and Maven Wrapper,
  with a non-root JRE runtime and Actuator healthcheck;
- `.dockerignore` excluding credentials, Git metadata and host build artifacts;
- Docker Compose services for the backend and local PostgreSQL, preserving the
  PostgreSQL persistent volume and healthcheck;
- backend startup dependent on PostgreSQL health, using `postgres:5432` internally
  and publishing backend port 8080 locally;
- environment-based configuration with `.env` gitignored and a secret-free `.env.example`;
- tracked `LOCAL_SETUP.md` covering host Spring Boot + Docker PostgreSQL and the full
  containerized stack;
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
- `./mvnw verify` passes;
- `docker compose config`, `docker compose build`, and `docker compose up` succeed;
- PostgreSQL and backend containers become healthy;
- `http://localhost:8080/actuator/health` returns `UP`;
- Flyway runs successfully during container startup and backend database connections
  use the Compose network and service name `postgres`, never host localhost;
- both documented development workflows remain supported;
- application starts against Docker PostgreSQL;
- Flyway executes successfully;
- no real credentials are committed;
- health endpoint works;
- Swagger/OpenAPI works in local development;
- code follows package-by-feature principles established in the docs.
