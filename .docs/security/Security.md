# PlaneR Security

- [Access Control](#access-control)
- [Authentication](#authentication)
  - [Password Storage](#password-storage)
  - [Password Policy](#password-policy)
  - [Access Tokens](#access-tokens)
  - [Refresh Tokens](#refresh-tokens)
  - [One-Time Tokens](#one-time-tokens)
  - [Login and Enumeration](#login-and-enumeration)
- [Email Verification](#email-verification)
- [Password Recovery](#password-recovery)
- [Email Change](#email-change)
- [Password Change](#password-change)
- [Account Deletion](#account-deletion)
- [Authorization](#authorization)
- [Client-Side Token Storage](#client-side-token-storage)
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

Verification uses the library's constant-time comparison. Stored hashes carry their cost factor, so raising it later only affects new hashes; a user's hash is upgraded transparently on their next successful login.

### Password Policy

- **Length: 8 to 72 bytes (UTF-8).** BCrypt silently ignores everything after 72 bytes, so the server rejects longer passwords instead of truncating them.
- **No composition rules.** Requiring a letter and a digit pushes users toward predictable patterns without making passwords meaningfully harder to guess; length matters more.
- **Common-password check.** Passwords found in a bundled list of the most common passwords are rejected.

The same policy applies at registration, password change, and password reset. The rules live in `planer-shared` so clients can give immediate feedback; the server is the source of truth.

### Access Tokens

The server issues a **JSON Web Token (JWT)** on successful login, registration, and refresh. The token is sent with every subsequent request as a **Bearer Token**:

```
Authorization: Bearer <token>
```

**Algorithm:** HS256
**Signing key:** at least 32 bytes, loaded from the environment, never committed
**Lifetime:** 15 minutes

The payload contains:

| Claim | Description |
|---|---|
| `sub` | The user ID, used for ownership checks |
| `jti` | The unique token ID |
| `iat` | Issued-at timestamp |
| `exp` | Expiration timestamp |
| `iss` | Issuer of the application |
| `aud` | Audience of the application |

Access tokens are deliberately short-lived and **not individually revocable**: logout and password changes revoke refresh tokens, and the access token simply expires within 15 minutes. There is no deny-list. If instant revocation is ever needed, it would be added then.

The signing key is rotated if it is ever suspected of being compromised. Rotating the key invalidates all existing access tokens, forcing every client to refresh. Refresh tokens are unaffected, because they are not signed with this key.

### Refresh Tokens

To keep a user signed in without a login every 15 minutes, the server also issues a **refresh token** alongside the access token.

Refresh tokens are:

- **Opaque strings** — 256 bits from a cryptographically secure random generator, not JWTs and not UUIDs
- **Stored hashed** — only the SHA-256 of the token is stored in the `refresh_tokens` table, so a database leak does not yield usable tokens
- **Valid for 60 days**, counted from the most recent use (each rotation starts a new 60 days)
- **Rotated on every use** — each refresh issues a new refresh token and invalidates the old one
- **Grouped in families** — every token descends from one login, and all tokens from that login share a `family_id`
- **Bound to the user** — a refresh token is only valid for the user it was issued to

**Reuse detection.** If a refresh token that has already been rotated is presented again, someone is holding a copy — either the legitimate client or a thief. The server cannot tell which, so it revokes the **entire family**, ending that session on every device that descends from it. The next request from the legitimate client then fails and sends the user to login.

**Revocation triggers:**

- Logout (the presented token's family)
- Password change (every family except the current session's)
- Password reset (every family)
- Account deletion (every family)
- Reuse detection (the affected family)
- Manual revocation from a future "sessions" screen

Once revoked, a refresh token cannot be used again, and a new login is required.

### One-Time Tokens

Email verification and password reset use **one-time tokens**. They are *not* JWTs and are not signed with `JWT_SECRET_KEY`.

- **Opaque** — 256 bits from a cryptographically secure random generator, delivered only in the emailed link
- **Stored hashed** — only the SHA-256 is stored, in a `one_time_tokens` table, together with the user, the purpose (`VERIFY_EMAIL` or `RESET_PASSWORD`), the email address concerned, the expiry, and a used-at timestamp
- **Single-use** — redeeming sets `used_at`; a token with `used_at` set is rejected
- **Purpose-bound** — a token is only accepted by the endpoint matching its purpose
- **Superseded** — requesting a new token for the same user and purpose invalidates earlier ones

Keeping these tokens out of the JWT world removes a class of mistakes: a verification or reset token can never be mistaken for an access token, since it is not a token the signing key can validate. It also means a reset token never has to carry anything derived from the user's password.

Expired and used rows are deleted by a daily cleanup job.

### Login and Enumeration

Login failures return the same response (`INVALID_CREDENTIALS`) whether the email does not exist or the password is wrong. When the email does not exist, the server still performs a BCrypt comparison against a dummy hash, so response time does not reveal which case occurred.

**Known trade-off:** `POST /api/auth/register` returns `409 CONFLICT` for an already-registered email, which does reveal that the address has an account. The forgot-password and resend endpoints are neutral, but registration is not. Hiding this would require the sign-up flow to always say "check your email", which hurts the experience for a personal app. The exposure is limited by [rate limiting](#rate-limiting) on registration.

## Email Verification

On registration, the account is created with `emailVerified = false` and a **verification email** is sent to the registered address.

The verification token is a [one-time token](#one-time-tokens) valid for **24 hours**.

**Unverified accounts** can use the application fully. The single restriction is that they **cannot change their email address**, since the stored address has never been confirmed. Verification matters mostly for password recovery: an unverified address is a weak recovery channel. If more restrictions are needed later, they are returned as `403 FORBIDDEN`.

A separate **resend** endpoint exists for cases where the original email was lost. It is rate-limited to one send per minute per email address.

**Email enumeration protection:** the resend response is identical whether or not the email is registered, so an attacker cannot probe which addresses exist.

## Password Recovery

If a user forgets their password, they can request a reset link via `POST /api/auth/forgot-password`. If the email is registered, a reset email is sent.

The reset token is a [one-time token](#one-time-tokens) valid for **1 hour**. Requesting a new reset link invalidates any earlier unused one.

Redeeming the token via `POST /api/auth/reset-password` sets a new password and **revokes every refresh token** for that user, forcing every device to sign in again. This ensures that if an attacker had a stolen session, it is terminated by the password reset. Access tokens already issued remain valid until they expire (at most 15 minutes).

The forgot-password response is neutral, matching the email-verification pattern, to prevent enumeration.

## Email Change

Changing the email is treated as a sensitive operation because email is the login identifier and the recovery channel.

`PUT /api/users/me/email` requires the user's **current password** as confirmation, plus the new email address. The account must have a verified email.

**Flow:**

1. Client posts `newEmail` and `currentPassword`.
2. Server validates the password and checks that the new email is not already registered.
3. Server stores the new address in `User.pendingEmail` — the current address remains active.
4. Server sends a verification email to the new address, and a **notification to the old address** saying a change was requested. This is what lets a legitimate owner notice a hijacked session.
5. User clicks the link, which calls `POST /api/auth/verify-email` with the token.
6. On success, `email` is updated to match `pendingEmail`, `emailVerified` is set to `true`, `pendingEmail` is cleared, and every refresh token except the current session's is revoked.

Until step 5 completes, the user can still log in with their current email. The pending change can be cancelled with `DELETE /api/users/me/email`, and the verification email can be resent with `POST /api/users/me/email/resend`. If the verification token expires, the change is discarded: the cleanup job clears `pendingEmail` for users whose pending token has expired.

## Password Change

`PUT /api/users/me/password` requires the user's **current password** as confirmation, plus the new password.

On success:

- All refresh tokens **except the current session** are revoked
- The current session continues uninterrupted
- The new password must pass the same [policy](#password-policy) as registration

## Account Deletion

`POST /api/users/me/delete` requires the user's **current password** in the request body. It is a `POST` rather than a `DELETE` with a body, because some HTTP clients and proxies drop request bodies on `DELETE`.

On success, the account and everything it owns are deleted in a single transaction, and every refresh token is revoked. Deletion is irreversible and is not soft-deleted.

## Authorization

Every query is scoped by the `sub` claim of the JWT. In practice:

- Every table with user-owned data is associated with a user
- The user ID from the token is used to filter every read and write
- A request for another user's resource returns **`404 Not Found`**, not `403 Forbidden`

The **`404` vs `403`** choice is deliberate: returning 403 would confirm that the resource exists, leaking information about other users' data. For a single-user app the risk is theoretical, but the convention is respected so the behaviour is correct if sharing is ever added.

## Client-Side Token Storage

The access token lives in memory only. The **refresh token** is the long-lived credential and must be stored in the platform's secure storage:

| Client | Storage |
|---|---|
| Android | Android Keystore–backed encrypted storage |
| Desktop | The operating system's credential store (Windows Credential Manager, macOS Keychain, Linux Secret Service) |

Shared defines a `TokenStorage` interface and each client provides the implementation (see [Architecture](../architecture/Architecture.md)). Tokens are never written to plain files, logs, or crash reports. On logout, or when a refresh fails, the stored token is deleted.

## Transport Security

All communication between client and server must happen over **HTTPS** in production. The server is deployed behind a reverse proxy (Caddy) that terminates TLS and forwards requests over plain HTTP to the Ktor application on the internal network.

During local development, plain HTTP on `localhost` is acceptable. The Android debug build allows cleartext traffic to `10.0.2.2` for the emulator; release builds do not allow cleartext at all.

Because the server sits behind a proxy, it must read the client's real address from the `X-Forwarded-For` header, and **only when the request comes from the trusted proxy**. Without this, every client appears to come from the proxy's IP and per-IP rate limits would affect all users at once. See [Deployment](../infrastructure/Deployment.md#tls-and-reverse-proxy).

Response headers:

- `Strict-Transport-Security` — set by the reverse proxy, which is the component that knows TLS is in place
- `X-Content-Type-Options: nosniff` — set by the server on every response
- `Cache-Control: no-store` — set by the server on every authentication response, so tokens are never cached

PlaneR's API returns only JSON to its own app clients (there is no browser client), so browser-oriented headers such as `X-Frame-Options` and CORS are not needed.

## Input Validation

Every incoming request is validated before any database interaction occurs. Invalid requests are rejected with a `400` (`VALIDATION_ERROR` or `BAD_REQUEST`) and a structured error body (see [Error Handling](../architecture/Error-Handling.md)).

Validation rules live in `planer-shared` and are used on both client and server, so the same rule applies everywhere. The server always re-validates, regardless of what the client sent — the client check exists for UX, not security.

All database access uses **parameterised statements**, eliminating SQL injection without requiring manual escaping.

## Rate Limiting

Authentication endpoints are the most attractive target for abuse. These endpoints are rate-limited, and a request over the limit receives `429 RATE_LIMITED` with a `Retry-After` header:

| Endpoint | Limit | Purpose |
|---|---|---|
| `POST /api/auth/login` | 10 / 15 min per IP + email | Prevent brute-force credential attacks |
| `POST /api/auth/login` | 50 / 15 min per IP | Prevent one address trying many emails |
| `POST /api/auth/register` | 5 / hour per IP | Prevent account spam |
| `POST /api/auth/forgot-password` | 3 / hour per email, 10 / hour per IP | Prevent reset-email spam |
| `POST /api/auth/resend-verification` | 1 / minute per email, 10 / hour per IP | Prevent verification-email spam |
| `POST /api/users/me/email` and `/resend` | 3 / hour per account | Prevent email spam through the change flow |

The thresholds are the initial proposal and will be adjusted based on real usage. Implementation uses Ktor's `RateLimit` plugin, backed by an in-memory store, which is sufficient for a single-instance deployment. The counters reset when the server restarts; a deployment with more than one instance would need a shared store.

## Secrets

Secrets are never committed to the repository.

| Secret | Purpose |
|---|---|
| `JWT_SECRET_KEY` | Signs and verifies access tokens (the only thing it signs) |
| `DB_PASSWORD` | Database connection |
| Email delivery credentials | Sending verification and reset emails (once the delivery mechanism is chosen) |

One-time tokens and refresh tokens use no signing key: they are random values compared by hash.

For local development, secrets live in `.env` (gitignored). In production they live on the server host and are never stored in GitHub; see [Deployment](../infrastructure/Deployment.md#secrets) for where each one lives.
