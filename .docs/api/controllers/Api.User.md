# PlaneR Api — User

- [Get Current User](#get-current-user)
- [Update Current User](#update-current-user)
- [Change Email](#change-email)
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
  "displayName": "Alex Morgan",
  "avatarUrl": "https://cdn.planer.app/avatars/0192a3b4.png",
  "timezone": "Europe/Berlin",
  "createdAt": "2025-07-15T12:00:00Z",
  "updatedAt": "2025-07-16T09:30:00Z"
}
```

---

## Update Current User

Partially updates the current user's profile.
Only the fields provided are changed.

```js
PUT {{host}}/api/users/me
```

**Request**

```json
{
  "displayName": "Alex M.",
  "avatarUrl": "https://cdn.planer.app/avatars/0192a3b4.png",
  "timezone": "Europe/London"
}
```

| Field | Required | Notes |
|---|---|---|
| `displayName` | no | 1–60 chars |
| `avatarUrl` | no | Valid URL or `null` to clear |
| `timezone` | no | Valid IANA identifier |

**Response**

The updated user object, same shape as **Get Current User**.

Email changes are deliberately **not** handled here — see **Change Email** below.

---

## Change Email

Requests an email change.
The new address is stored as `pendingEmail` and a verification link is sent to it.
The current address remains active until the new one is verified.

While an email change is pending, the account cannot be marked as verified for
the new address. The user may cancel by re-authenticating with their password
and calling this endpoint again with `null`, or by waiting for the pending
verification token to expire.

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

| Field | Required | Notes |
|---|---|---|
| `newEmail` | yes | Valid email, unique, different from current |
| `currentPassword` | yes | Confirms the user's identity |

**Response**

```json
{
  "message": "Verification email sent to the new address.",
  "pendingEmail": "alex.morgan@example.com"
}
```

**Errors**

- `401 Unauthorized` if `currentPassword` is incorrect
- `409 Conflict` if `newEmail` is already registered

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

All refresh tokens except the current one are revoked, so other devices are signed out.

**Errors**

- `401 Unauthorized` if `currentPassword` is incorrect
- `400 Validation Error` if `newPassword` does not meet the strength requirements

---

## Delete Current User

Deletes the account and **all** owned data (calendars, events, tasks, and refresh tokens).
This action is irreversible.

```js
DELETE {{host}}/api/users/me
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

- `401 Unauthorized` if the password is incorrect