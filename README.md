# SpotNight — Backend

Backend-first application for discovering establishments that are open now and, in later milestones, receiving personalized recommendations.

## Current scope

The core backend is implemented as a Java Spring Boot modular monolith using Spring MVC.

Current responsibilities:

- user registration and authentication;
- authorization;
- establishment registration by authenticated users;
- establishment moderation by administrators;
- establishment categories;
- operating hours;
- public establishment discovery;
- PostgreSQL persistence;
- API documentation;
- testing and production-oriented engineering practices.

The recommendation system is intentionally deferred to a separate Python/FastAPI repository.

## Architecture

```text
Internet / future frontend
          |
          v
        Nginx
          |
          v
   Spring Boot MVC
          |
   +------+------+----------------+
   |             |                |
  Auth      Establishments     Discovery
   |             |
   |         Moderation
   |
   +-------------+----------------+
                 |
                 v
             PostgreSQL
```

Later:

```text
Spring Boot
    |
    | HTTP
    v
FastAPI Recommendation Service
```

## Documentation

Read these documents before implementing features:

- `docs/PRODUCT_SCOPE.md`
- `docs/ARCHITECTURE.md`
- `docs/DOMAIN_MODEL.md`
- `docs/DATABASE_DESIGN.md`
- `docs/API_GUIDELINES.md`
- `docs/AUTHENTICATION.md`
- `docs/ESTABLISHMENT_LIFECYCLE.md`
- `docs/SECURITY.md`
- `docs/TESTING_STRATEGY.md`
- `docs/DEVELOPMENT.md`
- `docs/ROADMAP.md`

Architecture decisions are recorded under `docs/adr/`.

Codex task instructions are under `docs/codex/`.

## Repository principles

1. Keep the simplest architecture that satisfies the current requirements.
2. Do not introduce infrastructure without a concrete problem it solves.
3. Authentication remains inside the Spring Boot application.
4. The Java backend owns the business domain and persistent state.
5. The future FastAPI service owns recommendation/ranking computation only.
6. Database schema changes are managed exclusively with Flyway.
7. Business rules belong in services, not controllers.
8. JPA entities are never exposed directly through the API.
9. Public discovery only returns approved establishments.
10. Every business rule must have automated tests.
