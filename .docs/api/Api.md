# PlaneR Api

The Api is a RESTful HTTP interface served by the **Ktor** backend.
All endpoints are prefixed with `/api` and communicate in JSON.

- [Conventions](#conventions)
- [Versioning](#versioning)
- [Authentication](#authentication)
- [Authorization](#authorization)
- [Error Format](#error-format)
- [Pagination](#pagination)
- [Controllers](#controllers)

## Conventions

| Aspect | Value |
|---|---|
| Base URL (dev) | `http://localhost:8080/api` |
| Health endpoint | `http://localhost:8080/health` (no `/api` prefix) |
| Content type | `application/json` |
| Auth header | `Authorization: Bearer <token>` |
| ID format | UUID v7 (36-character string) |
| Dates | `yyyy-MM-dd` (e.g., `2025-07-15`) |
| Timestamps | ISO-8601 UTC (e.g., `2025-07-15T09:00:00Z`) |
| Timezone | Timestamps are always UTC; the client renders them in `User.timezone` |

Route parameters are written as `{id}`. Query parameters are optional unless marked otherwise.

### Dates vs Timestamps

- **Dates** are used for values that have no time component: `scheduledFor`, `from`/`to` range filters, and path parameters that address an occurrence.
- **Timestamps** are used for values that have a moment in time: `startsAt`, `endsAt`, `dueAt`, `createdAt`, `updatedAt`.

A timestamp always includes the timezone (`Z` for UTC). A date never does.

### Idempotency

`GET`, `PUT`, and `DELETE` requests are idempotent — repeating the same request produces the same result.

`POST` requests are **not** idempotent. Repeating a `POST` to a creation endpoint will create a second resource. Clients that retry after a network failure must first check whether the original request succeeded.

The bulk endpoints (`POST /api/tasks/bulk/*`, `POST /api/events/bulk/*`) share this property: they are not idempotent, and a retry may apply the operation twice. In practice this is harmless for complete/uncomplete, but the client should still avoid blind retries.

## Versioning

PlaneR does **not** version its API. The server and every client are built from
the same repository and updated in lockstep. There is no `/v1/` prefix and no
plan to add one — if a breaking change is needed, both sides change together
and clients are expected to update.

This is acceptable because PlaneR has no third-party API consumers. If external
integrations are ever added, this decision should be revisited.

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
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `POST /api/auth/verify-email`
- `POST /api/auth/resend-verification`
- `GET /health`

Every other endpoint returns `401 Unauthorized` if the token is missing, expired, or invalid.

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
  "code": "BAD_REQUEST",
  "message": "One or more fields are invalid.",
  "errors": {
    "email": ["must be a valid email address"],
    "password": ["must be at least 8 characters"]
  }
}
```

The `errors` map is optional and only present when the server can attribute the
failure to specific fields. When it is absent, the `message` field is the
primary explanation.

Common codes:

| Status | Code | Meaning |
|---|---|---|
| 400 | `BAD_REQUEST` | The request is malformed or contains invalid values |
| 401 | `UNAUTHORIZED` | Missing, expired, or invalid token |
| 403 | `FORBIDDEN` | Account-state failure (unverified, suspended, missing capability) |
| 404 | `NOT_FOUND` | Resource does not exist or is not owned by the caller |
| 409 | `CONFLICT` | Unique constraint violated, or a state-based conflict |
| 429 | `RATE_LIMITED` | Too many requests — see [Rate Limiting](#rate-limiting) |
| 500 | `INTERNAL_ERROR` | Unhandled server error |

**A note on `400`:** PlaneR uses `400 Bad Request` for both malformed requests
(JSON syntax errors, missing headers) and validation failures (field values that
do not satisfy the rules). This is a deliberate simplification — the `errors`
map in the body is what tells the client which case applies.

If a client needs to distinguish the two, it should check whether the `errors`
field is present: absent means the request was malformed, present means
specific fields failed validation.

## Pagination

Collection endpoints that can return an unbounded number of items use **page-based pagination**.

**Request parameters**

| Param | Type | Default | Notes |
|---|---|---|---|
| `page` | int | 0 | Zero-based page index |
| `size` | int | 20 | Items per page, max 100 |

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
- [Task](controllers/Api.Task.md) — to-do items
- [User](controllers/Api.User.md) — the authenticated user's profile and account