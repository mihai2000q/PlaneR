package app.planer.server.common.plugins

import app.planer.server.common.errors.ConflictException
import app.planer.server.common.errors.ForbiddenException
import app.planer.server.common.errors.InvalidCredentialsException
import app.planer.server.common.errors.MalformedRequestException
import app.planer.server.common.errors.NotFoundException
import app.planer.server.common.errors.RateLimitedException
import app.planer.server.common.errors.TokenExpiredException
import app.planer.server.common.errors.TokenInvalidException
import app.planer.server.common.errors.UnauthorizedException
import app.planer.server.common.errors.UpgradeRequiredException
import app.planer.server.common.errors.ValidationException
import app.planer.shared.common.responses.ErrorResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.header
import io.ktor.server.response.respond

/**
 * Turns failures into the shared error body, using Ktor's `StatusPages` plugin. See `Error-Handling.md` for the codes
 * and their statuses.
 */
fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.respondError(
                HttpStatusCode.BadRequest,
                ErrorResponse.VALIDATION_ERROR,
                "One or more fields are invalid.",
                cause.fields,
            )
        }
        // Raised by Ktor itself for invalid JSON or wrong types in a request body.
        exception<BadRequestException> { call, _ ->
            call.respondError(HttpStatusCode.BadRequest, ErrorResponse.BAD_REQUEST, "The request is malformed.")
        }
        exception<MalformedRequestException> { call, cause ->
            call.respondError(
                HttpStatusCode.BadRequest,
                ErrorResponse.BAD_REQUEST,
                cause.message ?: "The request is malformed.",
            )
        }
        exception<TokenInvalidException> { call, cause ->
            call.respondError(HttpStatusCode.BadRequest, ErrorResponse.TOKEN_INVALID, cause.message ?: "Invalid token.")
        }
        exception<TokenExpiredException> { call, cause ->
            call.respondError(HttpStatusCode.BadRequest, ErrorResponse.TOKEN_EXPIRED, cause.message ?: "Expired token.")
        }
        exception<UnauthorizedException> { call, cause ->
            call.respondError(HttpStatusCode.Unauthorized, ErrorResponse.UNAUTHORIZED, cause.message ?: "Unauthorized.")
        }
        exception<InvalidCredentialsException> { call, cause ->
            call.respondError(
                HttpStatusCode.Unauthorized,
                ErrorResponse.INVALID_CREDENTIALS,
                cause.message ?: "Invalid credentials.",
            )
        }
        exception<ForbiddenException> { call, cause ->
            call.respondError(HttpStatusCode.Forbidden, ErrorResponse.FORBIDDEN, cause.message ?: "Forbidden.")
        }
        exception<NotFoundException> { call, cause ->
            call.respondError(HttpStatusCode.NotFound, ErrorResponse.NOT_FOUND, cause.message ?: "Not found.")
        }
        exception<ConflictException> { call, cause ->
            call.respondError(HttpStatusCode.Conflict, ErrorResponse.CONFLICT, cause.message ?: "Conflict.")
        }
        exception<UpgradeRequiredException> { call, cause ->
            call.respondError(
                HttpStatusCode.UpgradeRequired,
                ErrorResponse.UPGRADE_REQUIRED,
                cause.message ?: "This version of PlaneR is no longer supported. Please update the app.",
            )
        }
        exception<RateLimitedException> { call, cause ->
            cause.retryAfterSeconds?.let { call.response.header(HttpHeaders.RetryAfter, it.toString()) }
            call.respondError(HttpStatusCode.TooManyRequests, ErrorResponse.RATE_LIMITED, "Too many requests.")
        }
        // Responses with no exception behind them: an unknown URL, or a known URL called with the wrong method.
        // (A response produced by an exception handler above never reaches these.)
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respondError(HttpStatusCode.NotFound, ErrorResponse.NOT_FOUND, "Resource not found.")
        }
        status(HttpStatusCode.MethodNotAllowed) { call, _ ->
            call.respondError(
                HttpStatusCode.MethodNotAllowed,
                ErrorResponse.METHOD_NOT_ALLOWED,
                "This method is not allowed for this endpoint.",
            )
        }
        // Anything unhandled: log the cause, never send it to the client.
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respondError(
                HttpStatusCode.InternalServerError,
                ErrorResponse.INTERNAL_ERROR,
                "Something went wrong.",
            )
        }
    }
}

private suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    code: String,
    message: String,
    errors: Map<String, List<String>>? = null,
) = respond(status, ErrorResponse(status.value, code, message, errors))
