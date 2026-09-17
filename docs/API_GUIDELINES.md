# REST API Guidelines — Onde é Hoje

## Base path

```text
/api/v1
```

## Serialization

- JSON property names use camelCase.
- IDs use UUID strings.
- timestamps use ISO-8601 UTC.
- operating times use local `HH:mm` values interpreted with the establishment timezone.

## Authentication endpoints

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

## Current user

```text
GET /api/v1/users/me
```

## Categories

```text
GET /api/v1/categories
```

Only active categories are public.

## Public establishments

```text
GET /api/v1/establishments
GET /api/v1/establishments/{id}
```

Only `APPROVED` establishments are exposed.

Suggested filters:

```text
categoryId
openNow
latitude
longitude
radiusKm
q
page
size
sort
```

Validation rules:

- latitude and longitude must be provided together;
- radius requires coordinates;
- page size is bounded;
- sort values are allowlisted.

## Current user's establishments

```text
POST /api/v1/users/me/establishments

GET /api/v1/users/me/establishments
GET /api/v1/users/me/establishments/{id}

PUT /api/v1/users/me/establishments/{id}
PUT /api/v1/users/me/establishments/{id}/operating-hours

POST /api/v1/users/me/establishments/{id}/resubmit
```

Ownership is always derived from the authenticated principal.

## Admin moderation

```text
GET  /api/v1/admin/establishments
GET  /api/v1/admin/establishments/{id}

POST /api/v1/admin/establishments/{id}/approve
POST /api/v1/admin/establishments/{id}/reject
POST /api/v1/admin/establishments/{id}/suspend
POST /api/v1/admin/establishments/{id}/reactivate
```

## Standard status codes

```text
200 OK                  successful read/update
201 Created             resource created
204 No Content          successful no-body operation

400 Bad Request         malformed/invalid input
401 Unauthorized        missing/invalid authentication
403 Forbidden           authenticated but disallowed
404 Not Found           resource does not exist or should not be disclosed
409 Conflict            duplicate resource / invalid state / optimistic lock conflict
500 Internal Server Error unexpected backend failure
```

## Standard error object

```json
{
  "timestamp": "2026-09-15T19:45:12Z",
  "status": 409,
  "code": "INVALID_ESTABLISHMENT_STATE",
  "message": "Only rejected establishments can be resubmitted.",
  "path": "/api/v1/users/me/establishments/{id}/resubmit",
  "fieldErrors": []
}
```

Validation errors:

```json
{
  "timestamp": "2026-09-15T19:45:12Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed.",
  "path": "/api/v1/auth/register",
  "fieldErrors": [
    {
      "field": "email",
      "message": "must be a valid email address"
    }
  ]
}
```

Centralize mapping through `@RestControllerAdvice`.

## Pagination

Defaults:

```text
page=0
size=20
```

Maximum size:

```text
100
```

Example response:

```json
{
  "content": [],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

## DTO policy

Never expose JPA entities as API DTOs.

Use explicit request and response models.

Client input must never control server-managed fields such as:

- owner ID;
- moderation status;
- moderation actor;
- created/updated timestamps;
- optimistic-lock version unless explicitly required for concurrency semantics.

## Search

V1 search is deliberately simple.

Use PostgreSQL/JPA-supported filtering first.

Do not introduce Elasticsearch/OpenSearch without a requirement for advanced search behavior.
