# PlaneR Error Handling

This document describes how errors are produced, propagated, and presented
across the PlaneR stack — from a Ktor route, through the client, to the UI.

- [Overview](#overview)
- [Error Shape](#error-shape)
- [Error Codes](#error-codes)
- [Server Side](#server-side)
- [Client Side](#client-side)
- [Conventions](#conventions)

## Overview

Errors flow through four stages:

1. **Thrown** — a service or route throws a typed exception.
2. **Caught** — a Ktor plugin catches it and maps it to a status code and body.
3. **Serialized** — the error is returned as JSON using the shared shape below.
4. **Rendered** — the client deserializes it and surfaces a message to the user.

Because errors are structured, the client does not need to parse free-text
messages to decide what to display. The `code` field is the source of truth;
the `message` field is a fallback for logging.

## Error Shape

Every error response uses the same JSON body:

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

| Field | Type | Notes |
|---|---|---|
| `status` | int | HTTP status code, repeated in the body for convenience |
| `code` | string | Machine-readable identifier (see below) |
| `message` | string | Human-readable summary, safe to display |
| `errors` | object? | Present only with `VALIDATION_ERROR`; maps field name to a list of messages |

## Error Codes

This is the canonical list. [Api](../api/Api.md#error-format) carries the same table;
if the two ever differ, this is a bug in the docs.

| Code | Status | Meaning |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Well-formed request whose field values failed validation. Includes `errors`. |
| `BAD_REQUEST` | 400 | Malformed request: invalid JSON, wrong types, missing or invalid headers |
| `TOKEN_INVALID` | 400 | A verification or reset token is malformed, already used, or does not match |
| `TOKEN_EXPIRED` | 400 | A verification or reset token has expired |
| `UNAUTHORIZED` | 401 | Missing, expired, or invalid access token |
| `INVALID_CREDENTIALS` | 401 | Wrong email/password at login, or wrong current password when confirming a sensitive action |
| `FORBIDDEN` | 403 | Token valid, but the account state does not allow the action (e.g., unverified) |
| `NOT_FOUND` | 404 | Resource does not exist or is not owned by the user |
| `CONFLICT` | 409 | Unique constraint violated (e.g., duplicate email) or a state-based conflict |
| `UPGRADE_REQUIRED` | 426 | The client version is below the server's minimum |
| `RATE_LIMITED` | 429 | Too many requests. Response carries a `Retry-After` header. |
| `INTERNAL_ERROR` | 500 | Unhandled server error |

Codes are strings rather than HTTP statuses alone because the client often needs
to distinguish between different causes of the same status. For example, both
"wrong password" and "expired access token" are `401`, but they need opposite
reactions: `INVALID_CREDENTIALS` is shown to the user as a form error, while
`UNAUTHORIZED` makes the client refresh its token silently and, only if that
fails, send the user to login. Likewise `TOKEN_EXPIRED` offers "send me a new link",
whereas `TOKEN_INVALID` does not.

## Server Side

Errors are raised as typed exceptions from the service layer:

```kotlin
throw ValidationException(mapOf(
    "email" to listOf("must be a valid email address")
))

throw NotFoundException("Event not found")
```

A Ktor plugin (`StatusPages`) catches them and produces the JSON body:

```kotlin
install(StatusPages) {
    exception<ValidationException> { call, cause ->
        call.respond(
            HttpStatusCode.BadRequest,
            ErrorResponse(
                status = 400,
                code = "VALIDATION_ERROR",
                message = "One or more fields are invalid.",
                errors = cause.fields
            )
        )
    }
    // ...
}
```

Unhandled exceptions fall through to a generic 500 handler that logs the stack
trace and returns an `INTERNAL_ERROR` body with a neutral message. The stack trace
is never sent to the client.

Two errors are produced outside the service layer, by plugins that run earlier in
the request pipeline:

- **`UPGRADE_REQUIRED`** comes from the client-version check, which runs before
  authentication (see [Api → Versioning and Forced Updates](../api/Api.md#versioning-and-forced-updates)).
- **`RATE_LIMITED`** comes from the rate-limit plugin and always includes `Retry-After`.

### Ownership and `404` vs `403`

When a user requests a resource they do not own, the server returns **`404 Not Found`**,
not `403 Forbidden`. This is deliberate: returning 403 would confirm the resource
exists, leaking information about other users' data.

For a single-user app this matters less, but the convention is still respected
so the behaviour is correct if sharing is ever added.

## Client Side

The `ApiClient` in `planer-core` deserializes every failure into an `ApiError`
value:

```kotlin
sealed class ApiError {
    data class Validation(val fields: Map<String, List<String>>) : ApiError()
    data object BadRequest : ApiError()
    data object InvalidCredentials : ApiError()
    data object Unauthorized : ApiError()
    data object TokenInvalid : ApiError()
    data object TokenExpired : ApiError()
    data object Forbidden : ApiError()
    data object NotFound : ApiError()
    data class Conflict(val message: String) : ApiError()
    data object UpgradeRequired : ApiError()
    data class RateLimited(val retryAfterSeconds: Int?) : ApiError()
    data object Network : ApiError()                 // no response: offline, DNS, timeout
    data class Unknown(val status: Int, val message: String) : ApiError()
}
```

Callers receive a `Result<T>` and can pattern-match on the failure:

```kotlin
when (val result = api.login(email, password)) {
    is Result.Success -> navigateToCalendar()
    is Result.Failure -> when (val err = result.error) {
        is ApiError.Validation -> showFieldErrors(err.fields)
        ApiError.InvalidCredentials -> showMessage("Incorrect email or password")
        is ApiError.RateLimited -> showMessage("Too many attempts. Try again later.")
        ApiError.Network -> showMessage("Can't reach the server")
        else -> showMessage("Something went wrong")
    }
}
```

Two errors are handled **inside `ApiClient`** and never reach individual screens:

- **`Unauthorized`** — the client refreshes the access token once and retries the
  request. Only if the refresh fails does the caller see `Unauthorized`, which every
  client treats as "session ended, go to login".
- **`UpgradeRequired`** — the client stops sending requests and exposes it through a
  global signal. Each app observes that signal and shows its blocking update screen,
  independent of whichever screen made the request.

The mapping from HTTP status and `code` to `ApiError` is centralized so that
individual screens do not repeat the logic. Turning an `ApiError` into text and
widgets is each client's job; `planer-core` has no UI.

## Conventions

- **Never send stack traces to the client.** Log them server-side; return a neutral message.
- **Never trust the client.** Even if the client validates an input, the server re-validates. The client-side check exists for UX, not security.
- **Field names in `errors` match the request body.** If the request uses `displayName`, the error key is `displayName`, not `display_name` or `name`.
- **`message` is display-safe.** Assume it will be shown to the user verbatim, even though the UI often prefers its own copy.
- **Add a new `code` when a new failure mode is genuinely distinct.** Do not overload existing codes with different meanings — the client cannot distinguish them. Adding a code also means adding it to the table here, to [Api](../api/Api.md#error-format), and to `ApiError`.
