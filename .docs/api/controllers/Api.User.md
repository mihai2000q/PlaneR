# PlaneR Api — User

- [Get Current User](#get-current-user)
- [Update Current User](#update-current-user)
- [Change Email](#change-email)
- [Cancel Email Change](#cancel-email-change)
- [Resend Email Change](#resend-email-change)
- [Change Password](#change-password)
- [Delete Current User](#delete-current-user)

All endpoints here require authentication and operate on the authenticated user only.

---

## Get Current User

```js
GET {{host}}/api/users/me
```

**Response**

```json
{
  "id": "0192a3b4-0000-7000-8000-000000000000",
  "email": "alex@example.com",
  "emailVerified": true,
  "pendingEmail": null,
  "displayName": "Alex Morgan",
  "avatarUrl": "https://example.com/avatars/alex.png",
  "timezone": "Europe/Berlin",
  "createdAt": "2025-07-15T12:00:00Z",
  "updatedAt": "2025-07-16T09:30:00Z"
}
```

`pendingEmail` is non-null while an [email change](#change-email) is awaiting verification.

---

## Update Current User

Partially updates the current user's profile.
Only the fields provided are changed.

```js
PATCH {{host}}/api/users/me
```

**Request**

```json
{
  "displayName": "Alex M.",
  "avatarUrl": "https://example.com/avatars/alex.png",
  "timezone": "Europe/London"
}
```

| Field         | Required | Notes                                                                                                                   |
|---------------|----------|-------------------------------------------------------------------------------------------------------------------------|
| `displayName` | no       | 1–60 chars                                                                                                              |
| `avatarUrl`   | no       | An `https` URL up to 500 chars, or `null` to clear. PlaneR does not host avatars; the URL points to an image elsewhere. |
| `timezone`    | no       | Valid IANA identifier                                                                                                   |

**Response**

The updated user object, same shape as **Get Current User**.

Changing `timezone` affects what "today" and "this week" mean from now on, and the
default timezone of items created afterwards. It does **not** move existing events
or tasks, which keep their own `timeZone` (see [Domain](../../domain/Domain.md#timezones)).

Email changes are deliberately **not** handled here — see **Change Email** below.

---

## Change Email

Requests an email change.
The new address is stored as `pendingEmail`. A verification link is sent to the new
address, and a notification is sent to the current one.
The current address remains active until the new one is verified.

The account's current email must itself be verified.

```js
PUT {{host}}/api/users/me/email
```

**Request**

```json
{
  "newEmail": "alex.morgan@example.com",
  "currentPassword": "Password123!"
}
```

| Field             | Required | Notes                                       |
|-------------------|----------|---------------------------------------------|
| `newEmail`        | yes      | Valid email, unique, different from current |
| `currentPassword` | yes      | Confirms the user's identity                |

Calling this again while a change is pending replaces the pending address and
invalidates the earlier link.

**Response**

```json
{
  "message": "Verification email sent to the new address.",
  "pendingEmail": "alex.morgan@example.com"
}
```

The change completes when the user redeems the link through
[Verify Email](Api.Auth.md#verify-email). If the link expires, the change is discarded.

**Errors**

- `400 VALIDATION_ERROR` if `newEmail` is invalid or equals the current address
- `401 INVALID_CREDENTIALS` if `currentPassword` is incorrect
- `403 FORBIDDEN` if the current email is not verified
- `409 CONFLICT` if `newEmail` is already registered

---

## Cancel Email Change

Discards a pending email change. The current address is unaffected.

```js
DELETE {{host}}/api/users/me/email
```

**Response**

```json
{
  "message": "Email change cancelled."
}
```

Cancelling when nothing is pending is not an error.

---

## Resend Email Change

Sends the verification link for a pending email change again.
Rate-limited to 3 per hour per account.

```js
POST {{host}}/api/users/me/email/resend
```

**Response**

```json
{
  "message": "Verification email sent to the new address."
}
```

**Errors**

- `404 NOT_FOUND` if no email change is pending

---

## Change Password

```js
PUT {{host}}/api/users/me/password
```

**Request**

```json
{
  "currentPassword": "Password123!",
  "newPassword": "EvenBetterPassword456!"
}
```

**Response**

```json
{
  "message": "Password updated successfully."
}
```

All refresh tokens except the current session's are revoked, so other devices are signed out.

**Errors**

- `401 INVALID_CREDENTIALS` if `currentPassword` is incorrect
- `400 VALIDATION_ERROR` if `newPassword` does not meet
  the [password policy](../../security/Security.md#password-policy)

---

## Delete Current User

Deletes the account and **all** owned data (calendars, events, tasks, and refresh tokens).
This action is irreversible.

This is a `POST`, not a `DELETE`, because it carries a body and some HTTP clients
and proxies drop bodies on `DELETE`.

```js
POST {{host}}/api/users/me/delete
```

**Request**

```json
{
  "password": "Password123!"
}
```

**Response**

```json
{
  "message": "Account deleted."
}
```

**Errors**

- `401 INVALID_CREDENTIALS` if the password is incorrect
