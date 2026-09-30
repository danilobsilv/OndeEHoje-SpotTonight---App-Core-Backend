# Local setup (M2)

Both supported development modes use PostgreSQL 17.6, Flyway at Spring startup,
`ddl-auto=validate`, and `open-in-view=false`. M1 provides registration, login,
Bearer access tokens, cookie-based refresh/logout, and the current user profile.
M2 adds the public, database-backed active category list.

## Prerequisites and configuration

- Docker with a running Linux-container engine and Docker Compose v2 with `--wait` support.
- JDK 21 and `JAVA_HOME` for host development and Maven tests. The full container
  workflow builds with the JDK and Maven Wrapper inside Docker.
- Network access for initial Maven dependencies and container images.
- Local port 8080 available for the backend; PostgreSQL uses port 5432 by default.

Copy `.env.example` to `.env` and fill in `DB_USERNAME` and `DB_PASSWORD` with
local-only credentials. `.env` is gitignored and excluded from the Docker build
context. No credentials are embedded in the image.

Also set `JWT_SECRET` to a Base64-encoded cryptographically random key of at least
32 bytes. There is no application default. Generate one locally without committing it:

```powershell
$keyBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($keyBytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($keyBytes)
```

Keep that value in your gitignored `.env` for Compose and export it for host runs.
Restarting with a different key invalidates existing access tokens.
Defaults are `ACCESS_TOKEN_TTL=PT15M`, `REFRESH_TOKEN_TTL=P30D`, and
`AUTH_COOKIE_SECURE=true`. For a plain-HTTP local browser, explicitly set
`AUTH_COOKIE_SECURE=false` in `.env` (or the host environment). This is accepted
only with `local` active and `prod` absent. Production requires HTTPS and secure cookies.

| Variable | Host Spring Boot | Compose backend |
| --- | --- | --- |
| `DB_USERNAME`, `DB_PASSWORD` | Export matching database credentials | Read from shell or `.env` |
| `DB_PORT` | PostgreSQL's published host port; defaults to 5432 | Does not change the internal database port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/spotnight`; adjust for `DB_PORT` | Explicitly set to `jdbc:postgresql://postgres:5432/spotnight` |
| `SPRING_PROFILES_ACTIVE` | Use `local` | Defaults to `local`; use `prod` to disable Swagger |

Compose reads `.env` automatically; Spring Boot on the host does not. Shell
variables override `.env` values in Compose, so keep credentials consistent.
For passwords containing `$` or `#`, use single-quoted values in `.env`.

## A. Fast development: host Spring Boot + Docker PostgreSQL

Start only the database. If the containerized backend is already running, first
run `docker compose stop backend` to free port 8080.

```sh
docker compose up -d --wait postgres
```

Export the same credentials you set in `.env`, then start Spring Boot.

PowerShell:

```powershell
$env:DB_USERNAME = 'your-local-username'
$env:DB_PASSWORD = 'your-local-password'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/spotnight'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Bash:

```bash
export DB_USERNAME='your-local-username'
export DB_PASSWORD='your-local-password'
export DB_URL='jdbc:postgresql://localhost:5432/spotnight'
bash ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

If you change `DB_PORT`, update the port in the host `DB_URL` too. Stop the host
application with Ctrl+C before switching to the containerized backend.

## B. Full stack: backend container + PostgreSQL container

After filling in `.env`:

```sh
docker compose config --quiet
docker compose build
docker compose up -d --wait --wait-timeout 180
docker compose ps
docker compose logs backend
```

`config --quiet` validates without printing resolved credentials. The backend
waits for PostgreSQL's existing healthcheck. Docker's service DNS resolves
`postgres` on the private Compose network; the backend connects directly to port
5432 there, regardless of the database's published host port.

The backend is published at `127.0.0.1:8080`. Its healthcheck calls Actuator from
inside the container. Actuator includes the database health indicator; database
failure makes health return an unsuccessful HTTP status. Startup logs show Flyway
applying V0 through V3 on a fresh database, upgrading an M1 database with V3,
or finding an already migrated database up to date. M2 requires no volume reset.

Rebuild after source changes with `docker compose up -d --build --wait`.

## Endpoints and verification

The browser authentication contract and CSRF sequence are documented in
[AUTHENTICATION.md](docs/AUTHENTICATION.md#m1-api-contract-and-browser-csrf).

- Health: http://localhost:8080/actuator/health — expected `{"status":"UP"}`.
- Categories: http://localhost:8080/api/v1/categories — public JSON array of active
  categories, ordered by name and then slug, with only id/name/slug fields.
- Local Swagger UI: http://localhost:8080/swagger-ui/index.html.
- Local OpenAPI: http://localhost:8080/v3/api-docs.

PowerShell health check:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Bash health check:

```bash
curl --fail http://localhost:8080/actuator/health
```

Check internal connectivity without printing credentials:

```sh
docker compose exec backend printenv DB_URL
docker compose exec backend getent hosts postgres
docker compose exec backend id
```

Expected: the JDBC URL names `postgres:5432`, DNS returns the PostgreSQL container
address, and the backend runs with UID/GID 10001.

## Image design and validation

The build stage uses Eclipse Temurin Java 21 JDK and Maven Wrapper 3.9.11, with a
BuildKit Maven cache. The runtime uses the Java 21 JRE, packaged application and
curl for the HTTP healthcheck. Maven, the JDK, source code, tests and host build
artifacts are not copied into the runtime. Java runs directly as PID 1 under a
non-root user. The standalone image defaults to `prod`; local Compose selects
`local`. Java 21 base tags receive patch updates; use `docker compose build --pull`
to refresh them.

Image packaging skips tests because Testcontainers needs a Docker daemon. Run the
full suite separately before accepting changes; the Docker build is not a test
replacement:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

On Bash use `bash ./mvnw test` and `bash ./mvnw verify`. Tests use disposable
PostgreSQL containers, need no `.env`, and fail rather than skip when Docker is
unavailable. Container changes also require the Compose build/startup/health
checks above and inspection of Flyway startup logs.

## One-time M1 pre-merge schema cleanup

Before M1 was shared or merged, V1 was edited to replace the redundant email
indexes with the explicitly named `uk_users_email` UNIQUE constraint while
retaining the normalization CHECK. V0 and V2 did not change. A database that
already applied the old V1 has an obsolete Flyway checksum and must not simply
be restarted with the edited migration or have its checksum repaired.

For this branch's **disposable local development database only**, the hardening
validation recreated the Compose volume below. This deletes its local accounts
and refresh sessions. Keep `.env` so the recreated database uses the same local
configuration. The volume name is specific to this repository's default Compose
project; inspect its labels and verify the project before removing it.

```sh
docker volume inspect spotnight-app-core-backend_postgres_data
docker compose down
docker volume rm spotnight-app-core-backend_postgres_data
docker compose build
docker compose up -d --wait --wait-timeout 180
```

Fresh startup must apply all available migrations and pass Hibernate `ddl-auto=validate`.
Do not use this reset procedure on shared/staging/production databases. Once
migrations are shared, use new migrations instead of editing V1.

## Shutdown and data persistence

```sh
docker compose stop
```

`docker compose down` removes the containers and network but retains the existing
`postgres_data` named volume. Do not add `--volumes` unless you intend to delete
local database data. Keep the same Compose project name/directory to reuse that
volume. Changing `.env` credentials does not change credentials in an initialized
database; update the database credentials explicitly when needed.

This Compose file is for local development. Only health is exposed through
Actuator, and Swagger is local-profile-only. The production image still requires
deployment-specific database credentials and environment configuration.
