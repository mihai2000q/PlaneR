package app.planer.shared.common.responses

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/** The body of every error response. See `Error-Handling.md`. [errors] is present only for `VALIDATION_ERROR`. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ErrorResponse(
    val status: Int,
    val code: String,
    val message: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) // omitted when null, whichever Json configuration encodes it
    val errors: Map<String, List<String>>? = null,
) {

    companion object {
        const val VALIDATION_ERROR = "VALIDATION_ERROR"
        const val BAD_REQUEST = "BAD_REQUEST"
        const val TOKEN_INVALID = "TOKEN_INVALID"
        const val TOKEN_EXPIRED = "TOKEN_EXPIRED"
        const val UNAUTHORIZED = "UNAUTHORIZED"
        const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
        const val FORBIDDEN = "FORBIDDEN"
        const val NOT_FOUND = "NOT_FOUND"
        const val METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED"
        const val CONFLICT = "CONFLICT"
        const val UPGRADE_REQUIRED = "UPGRADE_REQUIRED"
        const val RATE_LIMITED = "RATE_LIMITED"
        const val INTERNAL_ERROR = "INTERNAL_ERROR"
    }
}
