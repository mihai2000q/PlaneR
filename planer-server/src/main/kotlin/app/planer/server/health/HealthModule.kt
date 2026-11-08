package app.planer.server.health

import app.planer.server.health.api.healthRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

/** The health feature: mounts `GET /health`. Called from `configureModules()`. */
fun Application.healthModule() {
    routing {
        healthRoutes()
    }
}
