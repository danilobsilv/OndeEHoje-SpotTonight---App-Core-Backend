# Authentication and Authorization

Authentication is implemented **inside the Spring Boot backend**.

There is no authentication microservice.

## V1 model

```text
Email + Password
      |
      v
Spring Security
      |
      +--> short-lived JWT access token
      |
      +--> long-lived opaque refresh token
```

## Access token

- JWT;
- short lived;
- passed through `Authorization: Bearer <token>`;
- contains only required claims;
- configurable expiration;
- signed by the backend.

HMAC signing is acceptable in V1 because the same backend issues and verifies access tokens.

## Refresh token

- cryptographically random opaque token;
- long-lived;
- raw value is never persisted;
- only the token hash is stored;
- revocable;
- rotated after successful refresh.

For browser clients, prefer:

```text
HttpOnly
Secure
SameSite
```

cookie storage for refresh tokens.

Exact SameSite/CORS behavior must match the deployed frontend architecture.

## Registration

Public registration:

- accepts name, email, password;
- normalizes email;
- rejects duplicates;
- hashes password;
- always creates role `USER`;
- never accepts roles from the request.

## Login

Login verifies credentials and issues:

- access token;
- refresh token.

Authentication failure should not reveal whether an email exists unless product requirements explicitly require that behavior.

## Refresh

Refresh flow:

```text
refresh token A
   |
validate hash + expiry + revocation
   |
revoke A
   |
generate B
   |
persist hash(B)
   |
return new access token + B
```

This is refresh-token rotation.

## Logout

Logout revokes the presented refresh token and clears the refresh cookie where applicable.

## Password hashing

V1 uses Spring Security `BCryptPasswordEncoder`.

Do not store plaintext passwords.

Do not log passwords.

## Roles

V1:

```text
USER
ADMIN
```

No public endpoint may promote a user to `ADMIN`.

Admins should initially be provisioned through a controlled bootstrap/seed/administrative process.

## Authorization

Two levels are used.

### Coarse endpoint authorization

Example:

```text
/admin/** -> ADMIN only
```

### Resource authorization

Ownership must be checked in the service layer.

A normal user may mutate only their own establishments.

When revealing the existence of another user's private resource would leak information, return `404` rather than `403`.

## M1 API contract and browser CSRF

M1 assumes a same-origin frontend/API deployment. Cross-origin CORS access is
not enabled. A future separate frontend origin requires an explicit allowlist
and a reviewed cookie/SameSite configuration; do not enable wildcard credentials.

1. Call `GET /api/v1/auth/csrf`, retaining cookies. Its no-store JSON response is
   `{"headerName":"X-XSRF-TOKEN","token":"..."}`.
2. Send that token in the named HTTP header on **every auth POST**, retaining the
   CSRF cookie. Tokens in query parameters/form fields are not accepted.
3. Register with `POST /api/v1/auth/register`: `name`, `email`, `password`.
   Returns 201 with the user profile; registration does not log in.
4. Login with `POST /api/v1/auth/login`: `email`, `password`.
   Returns `{"accessToken":"...","tokenType":"Bearer","expiresIn":900}`
   and sets the refresh cookie. Raw refresh tokens are never included in JSON.
5. Send `Authorization: Bearer <accessToken>` to `GET /api/v1/users/me`.
   Cookies alone cannot authenticate this endpoint.
6. POST to `/api/v1/auth/refresh` with the cookies and CSRF header; no JSON body
   is needed. Replace the access token and let the browser accept the replacement
   refresh cookie. Do not run multiple refresh requests for the same token.
7. POST to `/api/v1/auth/logout` with the cookies and CSRF header. Returns 204,
   revokes the presented refresh token and expires its cookie. Missing, unknown,
   or already revoked tokens also return 204.

User responses contain `id`, `name`, `email`, `status`, `roles`, and `createdAt`.
Unknown JSON fields are rejected, including client-supplied roles/status/IDs.
Names are trimmed and limited to 120 characters. Emails are stripped and
lowercased with Locale.ROOT and limited to 254 characters. Passwords require
12–72 characters and at most 72 UTF-8 bytes (BCrypt's limit); passwords are never
trimmed or normalized. BCrypt uses cost 12.

The refresh cookie is named `refresh_token`, host-only, HttpOnly, Secure,
SameSite=Strict, and scoped to `/api/v1/auth`. The CSRF cookie is host-only,
HttpOnly, Secure, SameSite=Strict, named `__Host-csrf`, and scoped to `/`.
The bootstrap endpoint exposes an XOR-masked CSRF value in JSON so JavaScript
does not need access to either cookie. Responses containing tokens are no-store.
Call the bootstrap endpoint again if the CSRF cookie has been lost.

CSRF remains enabled on registration/login (including login CSRF), refresh and
logout because browsers attach cookies automatically. SameSite is defense in
depth, not a replacement for the CSRF header. A malicious origin cannot read the
bootstrap response under the same-origin policy or send an accepted cross-origin
custom header without CORS permission. Production's __Host- prefix also prevents
subdomains from planting the CSRF cookie. XSS is outside CSRF's protection.

Auth routes have a separate Spring Security chain without resource-server
authentication: Spring's default Bearer CSRF exemption must not bypass checks on
cookie-based endpoints. An Authorization header on auth routes is ignored and
never exempts CSRF. The Bearer API chain accepts only Authorization-header JWTs,
has no session/form/basic authentication, and exempts its explicit API namespaces
from CSRF. Other routes remain denied; health and local-only Swagger remain public.

## M1 token and persistence details

- HS256 JWTs use `JWT_SECRET`, a Base64-encoded random key of at least 32 bytes.
  Claims are only `iss`, `aud`, `sub` (user UUID), `iat`, `exp`, and `roles`.
  The issuer is `onde-e-hoje` and audience is `onde-e-hoje-api`. Signature,
  algorithm, issuer, audience, required claims and expiry are validated, with no
  expiry grace. Time is supplied by the application's injectable Clock.
- `ACCESS_TOKEN_TTL` defaults to PT15M and must be 1 second through 30 minutes.
  `REFRESH_TOKEN_TTL` defaults to P30D, must exceed access TTL and cannot exceed
  90 days. Each rotation starts a new refresh lifetime.
- Refresh credentials contain 32 SecureRandom bytes encoded as unpadded Base64URL.
  Only their SHA-256 hex hashes are stored. High token entropy makes slow password
  hashing unnecessary for these tokens.
- Refresh and logout take a pessimistic row lock. Revocation and replacement are
  atomic service transactions; a concurrent/replayed refresh has at most one
  successful exchange. A failed issuance rolls back revocation and replacement.
- Reuse of a revoked token returns 401; token-family-wide revocation and absolute
  session lifetime are not part of M1. Expired/revoked rows remain persisted.
- Disabled users cannot log in, refresh, or read their current profile.
  Existing JWT role claims remain valid until expiry, including after logout,
  role changes, or disabling an account, as accepted in ADR-003. There is no
  per-request authentication database lookup or access-token revocation list.
- `AUTH_COOKIE_SECURE=false` is permitted only under local without prod. The
  local CSRF cookie is then named `csrf`; this is strictly for plain-HTTP local
  development. The default remains secure.
- Flyway V1 adds users/user_roles; V2 adds refresh_tokens, with database constraints
  for normalized unique emails, supported roles/statuses, password/token hash
  formats, foreign keys and token lifetime consistency. Public registration can
  create only USER. No admin provisioning endpoint or seeded credential is added.

Authentication errors retain the common API error schema: duplicate registration
is 409 EMAIL_ALREADY_EXISTS; invalid login is 401 INVALID_CREDENTIALS; unusable
refresh is 401 INVALID_REFRESH_TOKEN; missing/invalid JWT is 401 UNAUTHORIZED;
CSRF and authorization denial use 403 FORBIDDEN.
