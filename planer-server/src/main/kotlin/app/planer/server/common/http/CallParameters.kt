package app.planer.server.common.http

import app.planer.server.common.errors.ValidationException
import io.ktor.server.application.ApplicationCall
import kotlin.uuid.Uuid

/** The path parameter [name] as a UUID. Maps to `400 VALIDATION_ERROR` when it is missing or not a UUID. */
fun ApplicationCall.requirePathUuid(name: String): Uuid =
    parseUuid(parameters[name]) ?: throw invalidUuid(name)

private fun parseUuid(raw: String?): Uuid? = raw?.let { runCatching { Uuid.parse(it) }.getOrNull() }

private fun invalidUuid(name: String) = ValidationException(mapOf(name to listOf("must be a valid UUID")))
