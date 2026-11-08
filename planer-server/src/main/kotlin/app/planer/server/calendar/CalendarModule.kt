package app.planer.server.calendar

import app.planer.server.calendar.api.calendarRoutes
import app.planer.server.calendar.data.CalendarRepositoryImpl
import app.planer.server.calendar.domain.CalendarRepository
import app.planer.server.calendar.domain.CalendarService
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import io.ktor.server.routing.routing

/** The calendar feature: registers its dependencies and mounts its routes. Called from `Application.module()`. */
fun Application.calendarModule() {
    dependencies {
        provide<CalendarRepository> { CalendarRepositoryImpl() }
        provide<CalendarService> { CalendarService(resolve(), resolve(), resolve(), resolve()) }
    }

    val service: CalendarService by dependencies
    routing {
        calendarRoutes(service)
    }
}
