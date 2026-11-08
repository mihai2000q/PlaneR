package app.planer.server.common.plugins

import app.planer.server.calendar.calendarModule
import app.planer.server.health.healthModule
import io.ktor.server.application.Application

/** Loads the feature modules. Each one registers its own dependencies and mounts its own routes. */
fun Application.configureModules() {
    healthModule()
    calendarModule()
}
