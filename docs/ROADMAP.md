# Backend Roadmap

## M0 — Project foundation

Deliver:

- Spring Boot bootstrap;
- Java 21+;
- Maven Wrapper;
- Spring MVC;
- PostgreSQL dependency;
- Spring Data JPA;
- Flyway;
- Docker Compose PostgreSQL;
- Testcontainers;
- Actuator;
- OpenAPI;
- base configuration;
- standardized error response;
- correlation ID support.

No business feature implementation yet.

## M1 — Users and authentication

Deliver:

- User;
- user roles;
- RefreshToken;
- registration;
- login;
- JWT access token;
- refresh-token rotation;
- logout;
- `/users/me`;
- Spring Security;
- tests.

## M2 — Categories

Deliver:

- Category persistence;
- Flyway seed;
- public category listing.

## M3 — Establishment core

Deliver:

- Establishment persistence;
- ownership;
- create establishment;
- owner establishment list;
- owner establishment detail;
- owner updates;
- `PENDING_REVIEW` default;
- optimistic locking.

## M4 — Operating hours

Deliver:

- OperatingHours persistence;
- multiple intervals/day;
- overnight schedules;
- replace schedule endpoint;
- timezone-aware open-now calculation;
- Clock injection;
- tests.

## M5 — Moderation

Deliver:

- admin moderation queue;
- admin detail;
- approve;
- reject;
- owner resubmit;
- suspend;
- reactivate;
- moderation history;
- state transition tests;
- concurrency conflict handling.

## M6 — Discovery

Deliver:

- public approved establishment listing;
- public detail;
- category filter;
- basic text search;
- pagination;
- open-now filtering;
- coordinates;
- distance calculation;
- radius filtering where practical.

After M6, reassess whether PostGIS is justified.

## Future core milestones

Potential:

- user preferences;
- favorites;
- interactions;
- ratings/reviews;
- special/holiday hours;
- establishment change-request workflow;
- multi-manager establishment access.

## Recommendation milestone

Separate Python/FastAPI repository.

Potential evolution:

1. deterministic weighted ranking;
2. content-based recommendations;
3. behavior-based features;
4. collaborative filtering;
5. embeddings;
6. hybrid recommender.

The Java backend remains the business-domain owner.
