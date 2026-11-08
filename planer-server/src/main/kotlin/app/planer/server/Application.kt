package app.planer.server

import app.planer.server.calendar.data.CalendarRepositoryImpl
import app.planer.server.calendar.domain.CalendarService
import app.planer.server.plugins.*
import io.ktor.server.application.Application

/** Ktor module, referenced from `application.conf`. The server is started by `EngineMain`. */
fun Application.module() {
    configureDatabase()
    configureSerialization()
    configureRouting()
}
