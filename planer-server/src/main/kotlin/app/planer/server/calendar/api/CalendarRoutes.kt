package app.planer.server.calendar.api

import app.planer.server.auth.requireUserId
import app.planer.server.calendar.domain.CalendarService
import app.planer.shared.calendar.requests.CreateCalendarRequest
import app.planer.shared.common.responses.HttpMessageResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.calendarRoutes(service: CalendarService) {
    route("/api/calendars") {
        post {
            val userId = call.requireUserId()
            val request = call.receive<CreateCalendarRequest>()
            service.create(userId, request)
            call.respond(
                HttpStatusCode.Created,
                HttpMessageResponse("Calendar created successfully!")
            )
        }
    }
}
