package app.planer.server.common.validation

import app.planer.server.common.errors.ValidationException
import app.planer.shared.validation.toFieldErrors
import io.konform.validation.Validation

/**
 * Validates [value] against this schema and returns it, or throws a [app.planer.server.common.errors.ValidationException] carrying the field errors
 * (mapped to `400 VALIDATION_ERROR`).
 */
fun <T> Validation<T>.validateOrThrow(value: T): T {
    val errors = validate(value).toFieldErrors()
    if (errors.isNotEmpty()) throw ValidationException(errors)
    return value
}
