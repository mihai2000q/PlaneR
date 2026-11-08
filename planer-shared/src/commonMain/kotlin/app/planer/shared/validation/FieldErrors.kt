package app.planer.shared.validation

import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.ValidationResult

/**
 * The errors of this result as the `errors` object of a `VALIDATION_ERROR`: field name to its messages.
 * Empty when the result is valid.
 */
fun ValidationResult<*>.toFieldErrors(): Map<String, List<String>> =
    when (this) {
        is Valid<*> -> emptyMap()
        is Invalid -> errors.groupBy({ it.dataPath.removePrefix(".") }, { it.message })
    }
