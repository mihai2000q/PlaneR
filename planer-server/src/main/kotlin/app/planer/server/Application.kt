package app.planer.server

import app.planer.server.common.plugins.*
import io.ktor.server.application.Application

/** Ktor module, referenced from `application.conf`. The server is started by `EngineMain`. */
fun Application.module() {
    configureDatabase()
    configureSerialization()
    configureErrorHandling()
    configureRouting()
}
