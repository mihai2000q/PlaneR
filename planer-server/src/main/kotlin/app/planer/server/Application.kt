package app.planer.server

import app.planer.server.plugins.configureRouting
import app.planer.server.plugins.configureSerialization
import io.ktor.server.application.Application

/** Ktor module, referenced from `application.conf`. The server is started by `EngineMain`. */
fun Application.module() {
    configureSerialization()
    configureRouting()
}
