# Architecture — Onde é Hoje Backend

## Style

The backend is a **Spring Boot modular monolith using Spring MVC**.

The application is intentionally not microservice-based.

```text
Future Client
    |
    v
  Nginx
    |
    v
Spring Boot MVC
    |
    +-- Auth
    +-- User
    +-- Establishment
    +-- Category
    +-- Moderation
    +-- Discovery
    |
    v
PostgreSQL
```

## Request flow

```text
HTTP Request
    |
    v
Controller
    |
    v
Service
    |
    v
Repository
    |
    v
PostgreSQL
```

### Controller responsibilities

- HTTP request parsing;
- request validation;
- authenticated principal extraction;
- status codes;
- response mapping.

Controllers must not implement domain rules or access repositories directly.

### Service responsibilities

- business rules;
- authorization requiring resource ownership/context;
- state transitions;
- transaction boundaries;
- orchestration between repositories.

### Repository responsibilities

- persistence and query access;
- no business-state changes by naming convention such as `approveEstablishment()`.

### Model responsibilities

JPA entities represent persisted application state.

Enums represent constrained states/actions.

### DTO responsibilities

DTOs define the external API contract. JPA entities are never API request/response objects.

## Package organization

Use package-by-feature with MVC layers inside each feature.

```text
com.ondeehoje
├── auth/
├── user/
├── establishment/
├── category/
└── shared/
```

Each feature may contain:

```text
controller/
service/
repository/
model/
dto/
mapper/
```

where needed.

## Internal module dependencies

Approximate dependency direction:

```text
auth ----------> user

establishment -> user
establishment -> category

category ------ independent

shared -------- imported by features
```

Avoid reverse dependencies where possible.

## Transactions

Transactions are defined around application operations.

Example moderation transaction:

```text
BEGIN
  load establishment
  validate current state
  update current status
  append moderation event
COMMIT
```

If any operation fails, the entire transaction rolls back.

## Concurrency

`Establishment` uses optimistic locking.

Use a version column / JPA `@Version`.

Concurrent moderation or owner modifications must fail with a conflict instead of silently overwriting state.

## Recommendation-service boundary

Not implemented in V1.

Future flow:

```text
Spring
  |
  | candidate establishments + features
  v
FastAPI Recommendation Service
  |
  | ranked IDs + scores
  v
Spring
```

Spring owns:

- domain state;
- users;
- establishments;
- permissions;
- candidate generation;
- persistence.

FastAPI owns:

- feature transformation;
- scoring;
- ranking;
- recommendation models.

FastAPI must not directly own or mutate Spring database tables.
