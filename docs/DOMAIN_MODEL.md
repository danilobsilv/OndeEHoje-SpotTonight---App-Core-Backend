# Domain Model — Onde é Hoje

## User

Represents an authenticated account.

Suggested fields:

```text
id              UUID
name            String
email           String
passwordHash    String
status          UserStatus
createdAt       Instant
updatedAt       Instant
```

### UserStatus

```text
ACTIVE
DISABLED
```

### Roles

```text
USER
ADMIN
```

Ownership of establishments is not represented by a global role.

## RefreshToken

Represents a revocable long-lived authentication credential.

Suggested fields:

```text
id          UUID
user        User
tokenHash   String
expiresAt   Instant
revokedAt   Instant?
createdAt   Instant
```

The raw token is never persisted.

## Category

Represents the primary classification of an establishment.

Suggested fields:

```text
id          UUID
name        String
slug        String
active      boolean
createdAt   Instant
```

V1 uses one primary category per establishment.

Tags are deferred.

## Establishment

Central domain entity.

Suggested fields:

```text
id                  UUID
owner               User
category            Category

name                String
description         String?

phone               String?
websiteUrl          String?
instagramHandle     String?

street              String
number              String?
complement          String?
neighborhood        String?
city                String
state               String
postalCode          String?
countryCode         String

latitude            double
longitude           double
timeZone            String

status              EstablishmentStatus
version             long

createdAt           Instant
updatedAt           Instant
```

### EstablishmentStatus

```text
PENDING_REVIEW
APPROVED
REJECTED
SUSPENDED
```

Moderation status is different from whether the establishment is currently open.

## OperatingHours

Represents one opening interval.

Suggested fields:

```text
id                  UUID
establishment       Establishment
dayOfWeek           int
opensAt             LocalTime
closesAt            LocalTime
closesNextDay       boolean
createdAt           Instant
```

`dayOfWeek` follows ISO-8601:

```text
1 = Monday
...
7 = Sunday
```

Multiple intervals per day are supported.

Example:

```text
Monday
09:00-12:00
14:00-22:00
```

Overnight example:

```text
Friday
18:00-02:00
closesNextDay=true
```

No row for a day means closed that day.

## EstablishmentModerationEvent

Audit history of moderation state changes.

Suggested fields:

```text
id                  UUID
establishment       Establishment
actor               User

action              ModerationAction
fromStatus          EstablishmentStatus
toStatus            EstablishmentStatus

reason              String?
createdAt           Instant
```

### ModerationAction

```text
APPROVED
REJECTED
SUSPENDED
REACTIVATED
```

The establishment table stores current state; moderation events store history.

## Relationships

```text
User 1 ---- N Establishment

User 1 ---- N RefreshToken

User 1 ---- N EstablishmentModerationEvent (actor)

Category 1 ---- N Establishment

Establishment 1 ---- N OperatingHours

Establishment 1 ---- N EstablishmentModerationEvent
```

## Core invariants

- New establishment => `PENDING_REVIEW`.
- Only approved establishments are publicly discoverable.
- Only the owner or authorized admin may operate on non-public establishment resources.
- Ownership comes from the authenticated user, not client input.
- Rejected establishments may be resubmitted.
- Sensitive changes to approved establishments are restricted in V1.
- Operating hours are interpreted using the establishment timezone.
