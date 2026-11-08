package app.planer.server.plugins

import app.planer.server.health.api.healthRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        healthRoutes()
    }
}
