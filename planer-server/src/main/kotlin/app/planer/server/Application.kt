package app.planer.server

import app.planer.server.plugin.configureRouting
import app.planer.server.plugin.configureSerialization
import io.ktor.server.application.Application

/** Ktor module, referenced from `application.conf`. The server is started by `EngineMain`. */
fun Application.module() {
    configureSerialization()
    configureRouting()
}
