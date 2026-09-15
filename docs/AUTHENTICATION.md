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
