package app.planer.server.common.http

import app.planer.server.common.errors.MalformedRequestException
import io.ktor.server.application.ApplicationCall
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer

private val queryJson = Json { ignoreUnknownKeys = true }

/**
 * Binds the query parameters to a `@Serializable` class `T`, the way `receive<T>()` binds a JSON body.
 * Defaults of `T` apply to absent parameters, and enum values are matched by their `@SerialName`.
 * A value that cannot be bound (not a UUID, unknown enum value) maps to `400 BAD_REQUEST`.
 * Rules beyond binding belong in a Konform schema, run with `validateOrThrow`.
 */
inline fun <reified T> ApplicationCall.receiveQuery(): T = receiveQuery(serializer<T>())

fun <T> ApplicationCall.receiveQuery(serializer: KSerializer<T>): T {
    val parameters = JsonObject(
        request.queryParameters.entries().associate { (name, values) -> name to JsonPrimitive(values.first()) }
    )
    return try {
        queryJson.decodeFromJsonElement(serializer, parameters)
    } catch (e: SerializationException) {
        throw MalformedRequestException("Invalid query parameters: ${e.message}")
    } catch (e: IllegalArgumentException) {
        throw MalformedRequestException("Invalid query parameters: ${e.message}")
    }
}
