package app.planer.server.event

import app.planer.server.event.data.EventRepositoryImpl
import app.planer.server.event.domain.EventRepository
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies

/** The event feature. Only its dependencies for now; its routes come with the event endpoints. */
fun Application.eventModule() {
    dependencies {
        provide<EventRepository> { EventRepositoryImpl() }
    }
}
