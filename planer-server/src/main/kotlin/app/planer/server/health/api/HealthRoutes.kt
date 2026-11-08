package app.planer.server.health.api

import app.planer.shared.common.responses.HealthResponse
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.healthRoutes() {
    get("/health") {
        call.respond(HealthResponse(HealthResponse.HEALTHY))
    }
}
