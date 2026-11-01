# PlaneR Api

The Api is a RESTful HTTP interface served by the **Ktor** backend.
All endpoints are prefixed with `/api` and communicate in JSON.

- [Conventions](#conventions)
- [Health](#health)
- [Versioning and Forced Updates](#versioning-and-forced-updates)
- [Authentication](#authentication)
- [Authorization](#authorization)
- [Error Format](#error-format)
- [Pagination](#pagination)
- [Controllers](#controllers)

## Conventions

| Aspect          | Value                                                                                                                                                           |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Base URL (dev)  | `http://localhost:8080/api`                                                                                                                                     |
| Health endpoint | `http://localhost:8080/health` (no `/api` prefix)                                                                                                               |
| Content type    | `application/json`                                                                                                                                              |
| Auth header     | `Authorization: Bearer <token>`                                                                                                                                 |
| Client headers  | `X-Client-Platform: android \| desktop` and `X-Client-Version: <semver>` on every request (see [Versioning and Forced Updates](#versioning-and-forced-updates)) |
| ID format       | UUID v7 (36-character string)                                                                                                                                   |
| Dates           | `yyyy-MM-dd` (e.g., `2025-07-15`). A date used as a range bound or filter is interpreted in the user's timezone.                                                |
| Timestamps      | ISO-8601 UTC (e.g., `2025-07-15T09:00:00Z`)                                                                                                                     |
| Timezone        | Timestamps are always UTC; the client renders them in `User.timezone`                                                                                           |

Route parameters are written as `{id}`. Query parameters are optional unless marked otherwise.

### Dates vs Timestamps

- **Dates** are used for values that have no time component: `scheduledFor`, `from`/`to` range filters, and path
  parameters that address an occurrence.
- **Timestamps** are used for values that have a moment in time: `startsAt`, `endsAt`, `dueAt`, `createdAt`,
  `updatedAt`.

A timestamp always includes the timezone (`Z` for UTC). A date never does.

### Idempotency

`GET`, `PUT`, `PATCH`, and `DELETE` requests are idempotent — repeating the same request produces the same result.
`PATCH` bodies are applied as a **JSON merge patch**: fields present are set, fields absent are left alone, and an
explicit `null` clears a nullable field.

`POST` requests are **not** idempotent. Repeating a `POST` to a creation endpoint will create a second resource. Clients
that retry after a network failure must first check whether the original request succeeded.

The bulk endpoints (`POST /api/tasks/bulk/*`, `POST /api/events/bulk/*`) are `POST`s but their *effects* are naturally
repeatable: deleting, completing, scheduling, moving, and reordering produce the same end state when applied twice. The
exception is `bulk/shift`, which moves events by a delta and is therefore **not** safe to repeat blindly.

### Updates

Partial updates use `PATCH`. `PUT` is used only for endpoints that set a specific piece of state (`/complete`,
`/schedule`, `/position`, …). Every update that changes an event or task returns the **updated resource**, so the client
never has to guess server-computed fields such as `routineStartAt`.

## Health

```js
GET {{host}}/health
```

Reports whether the server and its database connection are working.
It has no `/api` prefix, requires no authentication, and is exempt from the
client-version check and from rate limiting, so monitoring tools can call it freely.

**Response**

```json
{ "status": "healthy" }
```

If the database is unreachable the server returns `503 Service Unavailable` with
`{ "status": "unhealthy" }`. The body deliberately carries no detail — the cause is
logged server-side, not exposed on a public endpoint.

## Versioning and Forced Updates

PlaneR does **not** version its API. There is no `/v1/` prefix and no plan to add
one. The server and every client live in the same repository, and PlaneR has no
third-party API consumers.

Because mobile users cannot be updated in lockstep with the server, PlaneR instead
has a **forced-update mechanism**: the server can refuse requests from clients that
are too old.

**How it works**

1. Every client sends two headers on every request (including anonymous ones):

   ```
   X-Client-Platform: android
   X-Client-Version: 0.2.1
   ```

2. The server holds a minimum supported version per platform, set through
   configuration (`MIN_CLIENT_VERSION_ANDROID`, `MIN_CLIENT_VERSION_DESKTOP`).
3. If the client's version is lower than the minimum for its platform, the server
   answers **`426 Upgrade Required`** with the shared error body:

   ```json
   {
     "status": 426,
     "code": "UPGRADE_REQUIRED",
     "message": "This version of PlaneR is no longer supported. Please update the app."
   }
   ```

4. On `UPGRADE_REQUIRED`, the client stops making requests and shows a blocking
   screen that sends the user to the app store or the Releases page.

**Rules**

- The version check runs **before** authentication, so an outdated client always
  gets `426`, never a confusing `401`.
- The check applies to every endpoint except `GET /health`.
- If a minimum is not configured, the check is disabled. This is the default in
  development, so `curl` and Postman work without extra headers.
- If a minimum is configured and the headers are missing or malformed, the request
  is rejected with `426`.
- **Prefer additive changes.** New optional fields, new endpoints, and new enum
  values should not require raising the minimum. Raising it is for breaking changes
  only.
- **Raise the minimum only after the new client is available to users.** Google Play
  review can lag behind a server deploy, and raising the minimum early locks every
  user out. Deploy order for a breaking change: publish the new clients, wait until
  they are live, then raise the minimum.

If external integrations are ever added, the decision not to version the API should
be revisited.

## Authentication

Endpoints that require authentication expect a **JWT** in the `Authorization` header:

```
Authorization: Bearer <token>
```

Tokens are obtained from [Auth](controllers/Api.Auth.md).
See [Security](../security/Security.md) for the token structure, signing algorithm, and expiration.

The following endpoints allow anonymous requests:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout` (it authenticates by the refresh token in its body, so it works even when the access token has
  expired)
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `POST /api/auth/verify-email`
- `POST /api/auth/resend-verification`
- `GET /health`

Every other endpoint returns `401 Unauthorized` if the token is missing, expired, or invalid.
A client that receives `401` with code `UNAUTHORIZED` should try to refresh its access
token once and, if that fails, send the user back to login.

### Rate Limiting

Authentication endpoints are rate-limited to protect against abuse.
A client that exceeds the limit receives `429 Too Many Requests` with the shared
error body and should back off before retrying.

The specific thresholds per endpoint are documented in
[Security → Rate Limiting](../security/Security.md#rate-limiting).

## Authorization

PlaneR is single-user. There are no roles and no sharing.
Authorization is reduced to a single rule:

> **A user may only read or modify resources they own.**

Every query is scoped by the `sub` claim from the JWT.

### `404` vs `403`

**Ownership failures return `404 Not Found`, not `403 Forbidden`.**

If a request addresses a resource that either does not exist or is not owned by
the caller, the server responds with `404` in both cases. This is deliberate:
returning `403` would confirm the resource exists, letting an attacker probe
which IDs are real. Returning the same response for both cases removes that
oracle. RFC 7231 §6.5.3 explicitly permits this.

**`403 Forbidden` is reserved for account-state failures**, where the resource
is not the problem but the caller's account is. Examples:

- The account's email is unverified and the action requires verification
- The account has been suspended
- The account lacks a capability required for the action

The distinction is: **`404` means "not yours or not there"; `403` means "yours, but you can't do this right now."**

## Error Format

All errors return a JSON body in the following shape:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "One or more fields are invalid.",
  "errors": {
    "email": ["must be a valid email address"],
    "password": ["must be at least 8 characters"]
  }
}
```

The `code` field is the source of truth; clients switch on it and never parse
`message`. The `errors` map is present **only** with `VALIDATION_ERROR`.

Codes:

| Status | Code                  | Meaning                                                                                                   |
|--------|-----------------------|-----------------------------------------------------------------------------------------------------------|
| 400    | `VALIDATION_ERROR`    | Well-formed request, but one or more field values failed validation. Includes `errors`.                   |
| 400    | `BAD_REQUEST`         | Malformed request: invalid JSON, wrong types, missing or invalid headers                                  |
| 400    | `TOKEN_INVALID`       | A verification or reset token is malformed, already used, or does not match                               |
| 400    | `TOKEN_EXPIRED`       | A verification or reset token has expired                                                                 |
| 401    | `UNAUTHORIZED`        | Missing, expired, or invalid access token                                                                 |
| 401    | `INVALID_CREDENTIALS` | Wrong email/password at login, or wrong current password when confirming a sensitive action               |
| 403    | `FORBIDDEN`           | Account-state failure (unverified, suspended, missing capability)                                         |
| 404    | `NOT_FOUND`           | Resource does not exist or is not owned by the caller                                                     |
| 409    | `CONFLICT`            | Unique constraint violated, or a state-based conflict                                                     |
| 426    | `UPGRADE_REQUIRED`    | Client version is below the minimum — see [Versioning and Forced Updates](#versioning-and-forced-updates) |
| 429    | `RATE_LIMITED`        | Too many requests — see [Rate Limiting](#rate-limiting). Includes a `Retry-After` header.                 |
| 500    | `INTERNAL_ERROR`      | Unhandled server error                                                                                    |

Verification and reset tokens are anonymous-endpoint inputs, so their failures use
`400` rather than `401`: a `401` on those endpoints would look like an expired
session and could trigger the client's refresh-or-logout logic.

New codes are added only when a failure mode is genuinely distinct; existing codes
are never overloaded. See [Error Handling](../architecture/Error-Handling.md) for how
clients map these to behaviour.

## Pagination

Collection endpoints that can return an unbounded number of items use **page-based pagination**.

**Range queries are not paginated.** A calendar or day view needs *every* item in its date range, so `GET /api/events`
and `GET /api/tasks?from=…&to=…` return the complete result for the range instead of a page. They are bounded another
way: the range may not exceed **366 days** (otherwise `400 VALIDATION_ERROR`), which also bounds the work of expanding
recurring items. Their responses are `{ "items": [ ... ] }` without the page fields.

**Request parameters**

| Param  | Type | Default | Notes                   |
|--------|------|---------|-------------------------|
| `page` | int  | 0       | Zero-based page index   |
| `size` | int  | 20      | Items per page, max 100 |

**Response envelope**

Every paginated response wraps its collection in the following shape:

```json
{
  "items": [ ... ],
  "page": 0,
  "size": 20,
  "totalItems": 143,
  "totalPages": 8
}
```

`items` is the array of resources. The exact resource shape is defined per endpoint.

Endpoints that are inherently bounded (for example, the list of calendars — a
user rarely has more than 20) are **not** paginated. Their response shape is
documented in the controller file.

## Controllers

- [Auth](controllers/Api.Auth.md) — registration, login, refresh, logout, email verification, password reset
- [Calendar](controllers/Api.Calendar.md) — colored groupings for events and tasks
- [Event](controllers/Api.Event.md) — time-bound occurrences
- [Search](controllers/Api.Search.md) — global search across events and tasks *(sketch — not final)*
- [Summary](controllers/Api.Summary.md) — derived values for the home screen: streak, focus now, progress, overdue
- [Task](controllers/Api.Task.md) — to-do items
- [User](controllers/Api.User.md) — the authenticated user's profile and account
