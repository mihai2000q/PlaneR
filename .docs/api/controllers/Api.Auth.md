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
None of these endpoints require an existing access token.

---

## Register

Creates a new user account.
A **default Calendar** named "Personal" is created automatically.
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

| Field | Required | Notes |
|---|---|---|
| `email` | yes | Valid email, unique |
| `password` | yes | Min 8 chars, at least 1 letter and 1 digit |
| `displayName` | yes | 1–60 chars |
| `timezone` | yes | Valid IANA identifier |

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
  "refreshToken": "9b1c7e2f-...",
  "expiresAt": "2025-07-22T12:00:00Z"
}
```

**Errors**

- `409 Conflict` if the email is already registered

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

- `401 Unauthorized` if the credentials are invalid

---

## Refresh

Exchanges a valid refresh token for a new access token and a rotated refresh token.

```js
POST {{host}}/api/auth/refresh
```

**Request**

```json
{
  "refreshToken": "9b1c7e2f-..."
}
```

**Response**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "d4f0a8c3-...",
  "expiresAt": "2025-07-22T12:00:00Z"
}
```

**Errors**

- `401 Unauthorized` if the refresh token is invalid, expired, or has been revoked

---

## Logout

Revokes the current refresh token.
The access token remains valid until it expires, but the client should discard it.

```js
POST {{host}}/api/auth/logout
```

**Request**

```json
{
  "refreshToken": "d4f0a8c3-..."
}
```

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
  "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
}
```

The token is delivered via the verification email as a link. The client extracts
it from the URL and posts it here.

**Response**

```json
{
  "message": "Email verified.",
  "emailVerified": true
}
```

**Errors**

- `400 Validation Error` if the token is malformed
- `401 Unauthorized` if the token is expired or already used

---

## Resend Verification

Sends a new verification email to the currently registered address.
Rate-limited to one send per minute per account.

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

Consumes the password reset token and sets a new password.
All existing refresh tokens are revoked as a precaution.

```js
POST {{host}}/api/auth/reset-password
```

**Request**

```json
{
  "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
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

- `400 Validation Error` if `newPassword` does not meet the strength requirements
- `401 Unauthorized` if the token is expired or already used