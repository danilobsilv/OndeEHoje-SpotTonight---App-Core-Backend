# Database Design — Onde é Hoje

Database: PostgreSQL.

Schema migrations: Flyway.

## Tables

### users

```text
id              UUID PK
name            VARCHAR(120) NOT NULL
email           VARCHAR(254) NOT NULL UNIQUE
password_hash   VARCHAR NOT NULL
status          VARCHAR NOT NULL
created_at      TIMESTAMPTZ NOT NULL
updated_at      TIMESTAMPTZ NOT NULL
```

Application normalizes emails before persistence.

### user_roles

```text
user_id         UUID NOT NULL FK users(id)
role            VARCHAR NOT NULL
PRIMARY KEY(user_id, role)
```

### refresh_tokens

```text
id              UUID PK
user_id         UUID NOT NULL FK users(id)
token_hash      VARCHAR NOT NULL UNIQUE
expires_at      TIMESTAMPTZ NOT NULL
revoked_at      TIMESTAMPTZ NULL
created_at      TIMESTAMPTZ NOT NULL
```

Recommended indexes:

```text
refresh_tokens(token_hash)
refresh_tokens(user_id)
```

### categories

```text
id              UUID PK
name            VARCHAR NOT NULL
slug            VARCHAR NOT NULL UNIQUE
active          BOOLEAN NOT NULL DEFAULT TRUE
created_at      TIMESTAMPTZ NOT NULL
```

Categories may be seeded using Flyway.

### establishments

```text
id                  UUID PK
owner_id            UUID NOT NULL FK users(id)
category_id         UUID NOT NULL FK categories(id)

name                VARCHAR(150) NOT NULL
description         TEXT NULL

phone               VARCHAR NULL
website_url         VARCHAR NULL
instagram_handle    VARCHAR NULL

street              VARCHAR NOT NULL
number              VARCHAR NULL
complement          VARCHAR NULL
neighborhood        VARCHAR NULL
city                VARCHAR NOT NULL
state               VARCHAR NOT NULL
postal_code         VARCHAR NULL
country_code        CHAR(2) NOT NULL

latitude            DOUBLE PRECISION NOT NULL
longitude           DOUBLE PRECISION NOT NULL
time_zone           VARCHAR NOT NULL

status              VARCHAR NOT NULL
version             BIGINT NOT NULL DEFAULT 0

created_at          TIMESTAMPTZ NOT NULL
updated_at          TIMESTAMPTZ NOT NULL
```

Checks:

```text
latitude between -90 and 90
longitude between -180 and 180
```

Recommended indexes:

```text
establishments(owner_id)
establishments(status)
establishments(category_id)
establishments(status, category_id)
```

Do not add PostGIS in V1.

### operating_hours

```text
id                  UUID PK
establishment_id    UUID NOT NULL FK establishments(id)
day_of_week         SMALLINT NOT NULL
opens_at            TIME NOT NULL
closes_at           TIME NOT NULL
closes_next_day     BOOLEAN NOT NULL
created_at          TIMESTAMPTZ NOT NULL
```

Check:

```text
day_of_week between 1 and 7
```

Recommended index:

```text
operating_hours(establishment_id)
```

Overlapping intervals are prevented at the application layer in V1.

### establishment_moderation_events

```text
id                  UUID PK
establishment_id    UUID NOT NULL FK establishments(id)
actor_user_id       UUID NOT NULL FK users(id)

action              VARCHAR NOT NULL
from_status         VARCHAR NOT NULL
to_status           VARCHAR NOT NULL

reason              TEXT NULL
created_at          TIMESTAMPTZ NOT NULL
```

Recommended index:

```text
establishment_moderation_events(establishment_id)
```

## Flyway policy

Recommended initial migrations:

```text
V1__create_users.sql
V2__create_refresh_tokens.sql
V3__create_categories.sql
V4__create_establishments.sql
V5__create_operating_hours.sql
V6__create_establishment_moderation_events.sql
```

Rules:

- never use Hibernate auto-update for schema evolution;
- shared/applied migrations are immutable;
- schema changes are new migrations;
- important application invariants should also be enforced by database constraints when practical.

## Deletion

Hard deletion of establishments is not part of V1.

Moderation uses state transitions instead.

An archive/deletion policy will be designed only when there is a concrete requirement.
