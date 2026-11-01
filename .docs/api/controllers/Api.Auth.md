# PlaneR Api — Auth

- [Register](#register)
- [Login](#login)
- [Refresh](#refresh)
- [Logout](#logout)
- [Verify Email](#verify-email)
- [Resend Verification](#resend-verification)
- [Forgot Password](#forgot-password)
- [Reset Password](#reset-password)

Handles registration, login, token lifecycle, email verification, and password recovery.
None of these endpoints require an existing access token. Access tokens last **15 minutes** and refresh tokens **60
days**, rotating on every use; see [Security](../../security/Security.md#authentication) for the full model.

All failures use the shared error body with the codes from [Api](../Api.md#error-format).

---

## Register

Creates a new user account.
A single **default Calendar** named "Personal" is created automatically; the other palette entries
in [Domain](../../domain/Domain.md#calendar) are only presets the client offers when the user creates calendars.
A verification email is sent to the provided address.

On success, an access token and a refresh token are returned.
The account is marked **unverified** until the email is confirmed — see
[Verify Email](#verify-email) for what this restricts.

```js
POST {{host}}/api/auth/register
```

**Request**

```json
{
  "email": "alex@example.com",
  "password": "Password123!",
  "displayName": "Alex Morgan",
  "timezone": "Europe/Berlin"
}
```

| Field         | Required | Notes                                                                                                                                         |
|---------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `email`       | yes      | Valid email, unique                                                                                                                           |
| `password`    | yes      | 8–72 bytes (UTF-8), not in the common-password list. No composition rules — see [Password Policy](../../security/Security.md#password-policy) |
| `displayName` | yes      | 1–60 chars                                                                                                                                    |
| `timezone`    | yes      | Valid IANA identifier                                                                                                                         |

**Response**

```json
{
  "user": {
    "id": "0192a3b4-0000-7000-8000-000000000000",
    "email": "alex@example.com",
    "emailVerified": false,
    "displayName": "Alex Morgan",
    "avatarUrl": null,
    "timezone": "Europe/Berlin",
    "createdAt": "2025-07-15T12:00:00Z"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "q3Zr8v0hN1m...",
  "expiresAt": "2025-07-15T12:15:00Z"
}
```

`expiresAt` is when the **access token** expires. The refresh token is an opaque
string; its expiry is managed by the server and is not part of the response.

**Errors**

- `400 VALIDATION_ERROR` if a field is invalid
- `409 CONFLICT` if the email is already registered. This reveals that the address has an account;
  see [Login and Enumeration](../../security/Security.md#login-and-enumeration).

---

## Login

Authenticates an existing user and issues a new token pair.

```js
POST {{host}}/api/auth/login
```

**Request**

```json
{
  "email": "alex@example.com",
  "password": "Password123!"
}
```

**Response**

Same shape as **Register**.

**Errors**

- `401 INVALID_CREDENTIALS` if the credentials are invalid

---

## Refresh

Exchanges a valid refresh token for a new access token and a rotated refresh token.

```js
POST {{host}}/api/auth/refresh
```

**Request**

```json
{
  "refreshToken": "q3Zr8v0hN1m..."
}
```

**Response**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "d4f0a8c3Kx9...",
  "expiresAt": "2025-07-15T12:30:00Z"
}
```

The old refresh token stops working immediately. Presenting an already-used
refresh token is treated as theft and revokes the whole session family — see
[Refresh Tokens](../../security/Security.md#refresh-tokens).

**Errors**

- `401 UNAUTHORIZED` if the refresh token is invalid, expired, or has been revoked

---

## Logout

Revokes the session the refresh token belongs to.
The access token remains valid until it expires (at most 15 minutes), but the client should discard it.

This endpoint is anonymous: it is authorised by the refresh token in the body, so
a client whose access token has already expired can still log out.

```js
POST {{host}}/api/auth/logout
```

**Request**

```json
{
  "refreshToken": "d4f0a8c3Kx9..."
}
```

An unknown or already-revoked token is not an error — the response is the same, so
logging out twice is harmless.

**Response**

```json
{
  "message": "Logged out successfully."
}
```

---

## Verify Email

Confirms ownership of the email address by redeeming the token sent during
registration or when the email was changed.

```js
POST {{host}}/api/auth/verify-email
```

**Request**

```json
{
  "token": "Zm9vYmFyYmF6cXV4..."
}
```

The token is an opaque one-time token (valid 24 hours) delivered via the
verification email as a link. The client extracts it from the URL and posts it here.

**Response**

```json
{
  "message": "Email verified.",
  "emailVerified": true
}
```

**Errors**

- `400 TOKEN_INVALID` if the token is malformed or already used
- `400 TOKEN_EXPIRED` if the token has expired

---

## Resend Verification

Sends a new verification email to the registered address of an unverified account.
Rate-limited to one send per minute per email address (and per IP).

This only concerns the account's primary email. To resend the link for a pending
email *change*, use [Resend Email Change](Api.User.md#resend-email-change).

```js
POST {{host}}/api/auth/resend-verification
```

**Request**

```json
{
  "email": "alex@example.com"
}
```

**Response**

```json
{
  "message": "If the email is registered and unverified, a new verification link has been sent."
}
```

The response is intentionally identical whether or not the email exists and is
unverified, to avoid revealing which emails are registered.

---

## Forgot Password

Sends a password reset link to the provided email address, if it is registered.

```js
POST {{host}}/api/auth/forgot-password
```

**Request**

```json
{
  "email": "alex@example.com"
}
```

**Response**

```json
{
  "message": "If the email is registered, a password reset link has been sent."
}
```

Same neutrality rule as **Resend Verification**.

---

## Reset Password

Consumes the password reset token (valid 1 hour) and sets a new password.
All existing refresh tokens are revoked as a precaution. The new password must meet
the same [policy](../../security/Security.md#password-policy) as registration.

```js
POST {{host}}/api/auth/reset-password
```

**Request**

```json
{
  "token": "Zm9vYmFyYmF6cXV4...",
  "newPassword": "EvenBetterPassword456!"
}
```

**Response**

```json
{
  "message": "Password has been reset. Please sign in with your new password."
}
```

**Errors**

- `400 VALIDATION_ERROR` if `newPassword` does not meet the strength requirements
- `400 TOKEN_INVALID` if the token is malformed or already used
- `400 TOKEN_EXPIRED` if the token has expired
