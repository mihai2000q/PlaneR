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
| `errors` | object? | Present only for validation errors; maps field name to a list of messages |

## Error Codes

| Code | Status | Meaning |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Request body or query failed validation |
| `UNAUTHORIZED` | 401 | Missing, expired, or invalid token |
| `FORBIDDEN` | 403 | Token valid but action not allowed |
| `NOT_FOUND` | 404 | Resource does not exist or is not owned by the user |
| `CONFLICT` | 409 | Unique constraint violated (e.g., duplicate email) |
| `INTERNAL_ERROR` | 500 | Unhandled server error |

Codes are strings rather than HTTP statuses alone because the client often needs
to distinguish between different causes of the same status. For example, both
"wrong password" and "expired token" return 401, but the client should treat them
differently — one prompts a re-login, the other is a validation hint.

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
trace and returns a `INTERNAL_ERROR` body with a neutral message. The stack trace
is never sent to the client.

### Ownership and `404` vs `403`

When a user requests a resource they do not own, the server returns **`404 Not Found`**,
not `403 Forbidden`. This is deliberate: returning 403 would confirm the resource
exists, leaking information about other users' data.

For a single-user app this matters less, but the convention is still respected
so the behaviour is correct if sharing is ever added.

## Client Side

The `ApiClient` in `planer-core` deserializes every non-2xx response into an
`ApiError` value:

```kotlin
sealed class ApiError {
    data class Validation(val fields: Map<String, List<String>>) : ApiError()
    data object Unauthorized : ApiError()
    data object Forbidden : ApiError()
    data object NotFound : ApiError()
    data class Conflict(val message: String) : ApiError()
    data class Unknown(val status: Int, val message: String) : ApiError()
}
```

Callers receive a `Result<T>` and can pattern-match on the failure:

```kotlin
when (val result = api.login(email, password)) {
    is Result.Success -> navigateToCalendar()
    is Result.Failure -> when (val err = result.error) {
        is ApiError.Validation -> showFieldErrors(err.fields)
        ApiError.Unauthorized -> showMessage("Incorrect email or password")
        else -> showMessage("Something went wrong")
    }
}
```

The mapping from HTTP status to `ApiError` is centralized so that individual
screens do not repeat the logic.

## Conventions

- **Never send stack traces to the client.** Log them server-side; return a neutral message.
- **Never trust the client.** Even if the client validates an input, the server re-validates. The client-side check exists for UX, not security.
- **Field names in `errors` match the request body.** If the request uses `displayName`, the error key is `displayName`, not `display_name` or `name`.
- **`message` is display-safe.** Assume it will be shown to the user verbatim, even though the UI often prefers its own copy.
- **Add a new `code` when a new failure mode is genuinely distinct.** Do not overload existing codes with different meanings — the client cannot distinguish them.