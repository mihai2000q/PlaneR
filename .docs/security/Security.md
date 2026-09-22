# PlaneR Security

- [Access Control](#access-control)
- [Authentication](#authentication)
  - [Password Storage](#password-storage)
  - [JWT Tokens](#jwt-tokens)
  - [Refresh Tokens](#refresh-tokens)
- [Email Verification](#email-verification)
- [Password Recovery](#password-recovery)
- [Email Change](#email-change)
- [Password Change](#password-change)
- [Authorization](#authorization)
- [Transport Security](#transport-security)
- [Input Validation](#input-validation)
- [Rate Limiting](#rate-limiting)
- [Secrets](#secrets)

## Access Control

Before gaining access to the application, the user must go through three stages:

- **Identification** — the user provides a way to be recognised. PlaneR uses **email** as the unique identifier.
- **Authentication** — the user proves they are who they claim to be, by presenting a valid password or token.
- **Authorization** — the server verifies that the authenticated user is permitted to perform the requested action on the requested resource.

Because PlaneR is single-user per account with no sharing, authorization is reduced to a single rule: *the user may only read or modify resources they own*. See [Authorization](#authorization) below.

## Authentication

### Password Storage

Passwords are **never** stored in plain text. When a user registers, the password is hashed with **BCrypt** (cost factor 12) before being persisted. The original password is discarded immediately and is never logged, cached, or transmitted again.

BCrypt is chosen because:

- **Adaptive** — the cost factor can be increased as hardware gets faster
- **Salted automatically** — each password gets its own salt, protecting against rainbow tables
- **Slow by design** — brute-force attacks are computationally expensive

Verification hashes the incoming password with the stored salt and compares using a constant-time comparison function to prevent timing attacks.

### JWT Tokens

The server issues a **JSON Web Token (JWT)** on successful login or registration. The token is sent with every subsequent request as a **Bearer Token**:

```
Authorization: Bearer <token>
```

**Algorithm:** HS256
**Signing key:** at least 32 bytes, loaded from the environment, never committed
**Lifetime:** 7 days by default

The payload contains:

| Claim | Description |
|---|---|
| `sub` | The user ID, used for ownership checks |
| `jti` | The unique token ID, used for revocation if needed |
| `iat` | Issued-at timestamp |
| `exp` | Expiration timestamp |
| `iss` | Issuer of the application |
| `aud` | Audience of the application |

The signing key is rotated if it is ever suspected of being compromised. Rotating the key invalidates all existing access tokens, forcing every client to re-authenticate.

### Refresh Tokens

Access tokens are short-lived by design. To keep a user signed in across sessions without forcing a re-login every 7 days, the server also issues a **refresh token** alongside the access token.

Refresh tokens are:

- **Opaque strings** (random UUIDs), not JWTs
- **Stored in the database** in a `refresh_tokens` table, so they can be revoked
- **Rotated on every use** — each refresh issues a new refresh token and invalidates the old one
- **Bound to the user** — a refresh token is only valid for the user who was issued it

**Revocation triggers:**

- Logout
- Password change (all other sessions are signed out)
- Password reset (all sessions are signed out)
- Manual revocation from a future "sessions" screen

Once revoked, a refresh token cannot be used again, and a new login is required.

## Email Verification

On registration, the account is created with `emailVerified = false` and a **verification email** is sent to the registered address.

The verification token:

- Is a signed JWT with a short lifetime (24 hours)
- Is **single-use** — once redeemed, it cannot be redeemed again
- Contains the user ID and the email being verified in its claims

Until `emailVerified` is `true`, the account is restricted — it cannot change its email, and (in the future) may be prevented from performing other sensitive operations. The exact set of restrictions is finalised when the verification flow is implemented.

A separate **resend** endpoint exists for cases where the original email was lost. It is rate-limited to one send per minute per account.

**Email enumeration protection:** the resend response is identical whether or not the email is registered, so an attacker cannot probe which addresses exist.

## Password Recovery

If a user forgets their password, they can request a reset link via `POST /api/auth/forgot-password`. If the email is registered, a reset email is sent.

The reset token:

- Is a signed JWT with a short lifetime (1 hour)
- Is **single-use** — redeemed tokens cannot be reused
- Contains the user ID and a hash of the current password in its claims, so a password change invalidates any outstanding tokens

Redeeming the token via `POST /api/auth/reset-password` sets a new password and **revokes every refresh token** for that user, forcing every device to sign in again. This ensures that if an attacker had a stolen session, it is terminated by the password reset.

The forgot-password response is neutral, matching the email-verification pattern, to prevent enumeration.

## Email Change

Changing the email is treated as a sensitive operation because email is the login identifier and the recovery channel.

`PUT /api/users/me/email` requires the user's **current password** as confirmation, plus the new email address.

**Flow:**

1. Client posts `newEmail` and `currentPassword`.
2. Server validates the password and checks that the new email is not already registered.
3. Server stores the new address in `User.pendingEmail` — the current address remains active.
4. Server sends a verification email to the new address.
5. User clicks the link, which calls `POST /api/auth/verify-email` with the token.
6. On success, `email` is updated to match `pendingEmail`, `emailVerified` is set to `true`, and `pendingEmail` is cleared.

Until step 5 completes, the user can still log in with their current email. If the pending verification expires, the change is discarded automatically.

## Password Change

`PUT /api/users/me/password` requires the user's **current password** as confirmation, plus the new password.

On success:

- All refresh tokens **except the current session** are revoked
- The current session continues uninterrupted
- The new password must pass the same strength checks as registration

## Authorization

Every query is scoped by the `sub` claim of the JWT. In practice:

- Every table with user-owned data is associated with a user
- The user ID from the token is used to filter every read and write
- A request for another user's resource returns **`404 Not Found`**, not `403 Forbidden`

The **`404` vs `403`** choice is deliberate: returning 403 would confirm that the resource exists, leaking information about other users' data. For a single-user app the risk is theoretical, but the convention is respected so the behaviour is correct if sharing is ever added.

## Transport Security

All communication between client and server must happen over **HTTPS** in production. The server is deployed behind a reverse proxy (Caddy or Nginx) that terminates TLS and forwards requests over plain HTTP to the Ktor application on the internal network.

During local development, plain HTTP on `localhost` is acceptable.

The server sets the following security-related headers on every response:

- `Strict-Transport-Security` — instructs clients to use HTTPS on subsequent visits
- `X-Content-Type-Options: nosniff` — prevents MIME-type sniffing
- `X-Frame-Options: DENY` — prevents clickjacking

## Input Validation

Every incoming request is validated before any database interaction occurs. Malformed requests are rejected with a `400 Bad Request` and a structured error body (see [Error Handling](../architecture/Error-Handling.md)).

Validation rules live in `planer-core` and are used on both client and server, so the same rule applies everywhere. The server always re-validates, regardless of what the client sent — the client check exists for UX, not security.

All database access uses **parameterised statements**, eliminating SQL injection without requiring manual escaping.

## Rate Limiting

Authentication endpoints are the most attractive target for abuse. The following endpoints are planned to be rate-limited:

| Endpoint | Limit | Purpose |
|---|---|---|
| `POST /api/auth/login` | 10 / 15 min per IP + email | Prevent brute-force credential attacks |
| `POST /api/auth/register` | 5 / hour per IP | Prevent account spam |
| `POST /api/auth/forgot-password` | 3 / hour per email | Prevent reset-email spam |
| `POST /api/auth/resend-verification` | 1 / minute per account | Prevent verification-email spam |

The specific thresholds are the initial proposal and will be adjusted based on real usage. Implementation uses Ktor's `RateLimit` plugin, backed by an in-memory store for a single-instance deployment.

## Secrets

Secrets are never committed to the repository.

| Secret | Purpose |
|---|---|
| `JWT_SECRET_KEY` | Signs and verifies access tokens and verification/reset tokens |
| `DB_PASSWORD` | Database connection |
| Verification/reset tokens | Derived from `JWT_SECRET_KEY` via HMAC — no separate key |

For local development, secrets live in `.env` (gitignored). For production, they are injected via environment variables from the deployment platform. See [Deployment](../infrastructure/Deployment.md#secrets) for where each one lives.