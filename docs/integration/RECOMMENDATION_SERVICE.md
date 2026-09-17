# Future Recommendation Service Boundary

This integration is **not part of backend V1**.

## Repositories

```text
onde-e-hoje-backend
    Java / Spring Boot

onde-e-hoje-recommendation
    Python / FastAPI
```

## Ownership

### Spring backend owns

- users;
- authentication/authorization;
- establishments;
- categories;
- operating hours;
- discovery filters;
- persistent business state;
- candidate generation.

### Recommendation service owns

- recommendation feature transformations;
- scoring;
- ranking;
- recommendation models;
- model evaluation logic.

## Integration direction

```text
Spring
  |
  | HTTP request
  v
FastAPI
  |
  | ranked IDs/scores
  v
Spring
```

FastAPI is an internal service.

Clients should not need to know FastAPI exists.

## Database rule

FastAPI must not directly mutate Spring-owned tables.

Prefer Spring sending the data/features needed for ranking.

## Initial future contract

Conceptually:

```text
POST /internal/v1/recommendations
```

Input may contain:

- user preference features;
- candidate establishment IDs;
- candidate attributes/features.

Output may contain:

- establishment ID;
- recommendation score;
- optional explanation metadata.

Exact schema will be designed when recommendation work begins.

## Reliability topics to design later

- connect timeout;
- read timeout;
- bounded retries only when safe;
- circuit breaker if justified;
- degraded/fallback behavior;
- service authentication;
- observability;
- correlation IDs;
- versioned API contract.
