package app.planer.server.common.errors

// One exception per error code of `Error-Handling.md`, each documented with the status it maps to.
// `INTERNAL_ERROR` (500) has none: it is the fallback for anything unhandled.

/** A field-level validation failure. Maps to `400 VALIDATION_ERROR`, with [fields] as the `errors` object. */
class ValidationException(val fields: Map<String, List<String>>) : RuntimeException("Validation failed: $fields")

/** A malformed request: invalid JSON, wrong types, missing or invalid headers. Maps to `400 BAD_REQUEST`. */
class MalformedRequestException(message: String) : RuntimeException(message)

/** A verification or reset token that is malformed, already used, or does not match. Maps to `400 TOKEN_INVALID`. */
class TokenInvalidException(message: String) : RuntimeException(message)

/** A verification or reset token that has expired. Maps to `400 TOKEN_EXPIRED`. */
class TokenExpiredException(message: String) : RuntimeException(message)

/** A missing, expired or invalid access token (or caller identity). Maps to `401 UNAUTHORIZED`. */
class UnauthorizedException(message: String) : RuntimeException(message)

/** Wrong email or password at login, or a wrong current password on a sensitive action. Maps to `401 INVALID_CREDENTIALS`. */
class InvalidCredentialsException(message: String) : RuntimeException(message)

/** A valid caller whose account state forbids the action (unverified, suspended, ...). Maps to `403 FORBIDDEN`. */
class ForbiddenException(message: String) : RuntimeException(message)

/** A resource that does not exist or is not owned by the caller. Maps to `404 NOT_FOUND`. */
class NotFoundException(message: String) : RuntimeException(message)

/** A unique constraint or state-based conflict. Maps to `409 CONFLICT`. */
class ConflictException(message: String) : RuntimeException(message)

/** A client version below the minimum for its platform. Maps to `426 UPGRADE_REQUIRED`. */
class UpgradeRequiredException(message: String) : RuntimeException(message)

/** Too many requests. Maps to `429 RATE_LIMITED`, with [retryAfterSeconds] as the `Retry-After` header when known. */
class RateLimitedException(val retryAfterSeconds: Int? = null) : RuntimeException("Too many requests.")
