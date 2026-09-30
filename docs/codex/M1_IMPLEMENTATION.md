# M1 implementation report

Implemented on `feat/m1-authentication`. No merge into main was made.
Scope is users/authentication only; no M2 or later features were added.

## Behavior and design

- Registration returns a profile (201), always creates ACTIVE/USER, normalizes
  email, rejects duplicate email (409), and uses BCrypt cost 12. Client-supplied
  roles and other unknown fields are rejected (400).
- Login returns an access JWT and sets an HttpOnly refresh cookie. Unknown email,
  wrong password and disabled accounts share the same 401 credentials error.
- Access JWTs use HS256 with an externally supplied Base64 key of at least 32
  bytes. Minimal claims: issuer, audience, UUID subject, issued-at, expiry, roles.
  Default lifetime is 15 minutes. Verification enforces signature/algorithm,
  issuer, audience, required claims and expiration with the injectable Clock.
- Refresh tokens contain 32 cryptographically random bytes, encoded as Base64URL.
  Only SHA-256 hashes are persisted. Default lifetime is 30 days per rotation.
  Refresh locks the row and atomically revokes/replaces it. Concurrent exchanges
  have one winner, and failures roll back the transaction.
- Logout revokes the presented refresh token, clears the cookie and returns 204.
  Access JWTs remain valid until expiry, as specified by ADR-003.
- `GET /api/v1/users/me` requires Bearer authentication and returns a DTO.
  Admin routes require ADMIN; no production admin endpoint or admin seed was added.
- Existing error format, request IDs, Actuator exposure, local-only Swagger,
  JPA validation, Flyway startup and container design are retained.

## CSRF

Auth endpoints use a separate filter chain without resource-server authentication.
All auth POSTs require the CSRF cookie and bootstrap token in an HTTP header,
including registration/login, refresh and logout. A Bearer header never exempts
them. This separation is necessary because Spring's resource-server default
exempts Bearer requests from CSRF.

`GET /api/v1/auth/csrf` supplies an XOR-masked token in no-store JSON. Secure
deployments use a host-only, HttpOnly, Secure, SameSite=Strict `__Host-csrf`
cookie. Refresh cookies are also HttpOnly/Secure/SameSite=Strict and scoped to
`/api/v1/auth`. Only the explicit Bearer API namespaces are configured as CSRF
exempt; cookies cannot authenticate those endpoints.

M1 assumes same-origin deployment with no cross-origin CORS permission. SameSite
is additional protection, not the sole defense. See
[the authentication contract](../AUTHENTICATION.md#m1-api-contract-and-browser-csrf)
for the client sequence and production/local configuration.

## Flyway

- [V1__create_users.sql](../../src/main/resources/db/migration/V1__create_users.sql):
  users and user_roles; normalized/case-insensitive unique email protection,
  supported status/roles, BCrypt hash format, primary/foreign keys.
- [V2__create_refresh_tokens.sql](../../src/main/resources/db/migration/V2__create_refresh_tokens.sql):
  refresh tokens; unique SHA-256 hashes, user foreign key/index and timestamp checks.
- V0 was not changed.

## Files created

Paths below are relative to `src/main/java/com/spotnight/`:

- `auth/controller/AuthController.java`
- `auth/dto/AccessTokenResponse.java`
- `auth/dto/LoginRequest.java`
- `auth/dto/RegisterRequest.java`
- `auth/model/RefreshToken.java`
- `auth/repository/RefreshTokenRepository.java`
- `auth/security/AuthProperties.java`
- `auth/security/TokenConfiguration.java`
- `auth/service/AccessTokenService.java`
- `auth/service/AuthService.java`
- `auth/service/RefreshTokenCodec.java`
- `user/controller/UserController.java`
- `user/dto/UserResponse.java`
- `user/model/Role.java`
- `user/model/User.java`
- `user/model/UserStatus.java`
- `user/repository/UserRepository.java`
- `user/service/UserService.java`
- `shared/error/ApiException.java`

Other new files:

- The two Flyway migrations above.
- `src/test/java/com/spotnight/auth/AuthIntegrationTest.java`
- `src/test/java/com/spotnight/auth/RefreshTokenCodecTest.java`
- `src/test/java/com/spotnight/auth/security/TokenConfigurationTest.java`
- `docs/codex/M1_IMPLEMENTATION.md` (this report).

## Files modified

- `pom.xml`: Spring Security OAuth2 resource-server starter for its standard
  Bearer filter and Nimbus JWT signing/verification.
- `src/main/java/com/spotnight/shared/config/SecurityConfiguration.java`
- `src/main/java/com/spotnight/shared/error/GlobalExceptionHandler.java`
- `src/main/resources/application.yml`
- `src/test/resources/application-test.yml`
- `src/test/java/com/spotnight/FoundationIntegrationTest.java`
- `src/test/java/com/spotnight/ProductionExposureTest.java`
- `compose.yaml`
- `.env.example` (pre-existing user edits preserved)
- `LOCAL_SETUP.md`
- `docs/AUTHENTICATION.md`

A gitignored local `.env` was generated for Compose validation with random
credentials, local profile and insecure cookies explicitly enabled for localhost
HTTP. It is excluded from Docker's build context. No real secret was added to
tracked files. The Dockerfile and persistent-volume definition were unchanged.

## Validation results — 2026-09-17

- Windows Maven Wrapper equivalents of `./mvnw test` and `./mvnw verify`:
  **29 tests, zero failures/errors/skips** in each successful run.
- Coverage includes every requested acceptance case, invalid/expired/tampered
  JWTs, disabled accounts, invalid DTOs, PostgreSQL constraints, CSRF failure
  modes, actual CSRF bootstrap/cookies, admin role separation, concurrent refresh
  and registration, and refresh rollback on signing failure.
- All M0 tests pass. Its schema assertion now expects Flyway V2 and the three
  M1 tables instead of a deliberately empty M0 schema.
- `docker compose config --quiet`, `docker compose build`, and
  `docker compose up -d --wait --wait-timeout 180`: passed.
- Both services healthy; health endpoint UP; Flyway applied V0/V1/V2 successfully.
- Docker networking resolves `postgres`; backend JDBC URL is
  `jdbc:postgresql://postgres:5432/spotnight`; backend UID/GID is 10001.
- Live HTTP checks through port 8080 passed for cookie attributes, registration,
  login, current profile, missing-CSRF rejection with Bearer, refresh rotation,
  old-token rejection, logout and revoked-token rejection.
- `git diff --check`: passed.

The local Compose stack remains running with its persistent volume.

## Assumptions and documented choices

The source docs did not fix token lifetimes, password limits, registration
response, transport details or CORS policy. These are now specified in
AUTHENTICATION.md: separate login after registration, 12-character minimum
password with BCrypt's 72-byte maximum, same-origin browser cookie transport,
15-minute/30-day default lifetimes, and an explicit CSRF bootstrap endpoint.

Spring's standard BearerTokenAuthenticationFilter fulfills the JWT filter
deliverable; no custom token parser/filter was necessary. No non-browser JSON
refresh-token transport, admin bootstrap flow, family-wide replay revocation,
expired-token cleanup, or immediate JWT revocation was added. These are not
required by M1; ADR-003's access-token expiry trade-off remains in effect.
