package app.planer.server.calendar.api

import app.planer.server.auth.requireUserId
import app.planer.server.calendar.domain.CalendarService
import app.planer.server.common.http.receiveQuery
import app.planer.server.common.http.requirePathUuid
import app.planer.shared.calendar.requests.CreateCalendarRequest
import app.planer.shared.calendar.requests.DeleteCalendarRequest
import app.planer.shared.calendar.requests.ReorderCalendarRequest
import app.planer.shared.calendar.requests.UpdateCalendarRequest
import app.planer.shared.calendar.responses.DeleteCalendarResponse
import app.planer.shared.common.responses.HttpMessageResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.calendarRoutes(service: CalendarService) {
    route("/api/calendars") {
        get {
            val userId = call.requireUserId()
            call.respond(service.getList(userId).toListResponse())
        }

        post {
            val userId = call.requireUserId()
            val request = call.receive<CreateCalendarRequest>()
            service.create(userId, request)
            call.respond(HttpStatusCode.Created, HttpMessageResponse("Calendar created."))
        }

        route("/{id}") {
            get {
                val userId = call.requireUserId()
                val id = call.requirePathUuid("id")
                call.respond(service.get(userId, id).toResponse())
            }

            put {
                val userId = call.requireUserId()
                val id = call.requirePathUuid("id")
                val request = call.receive<UpdateCalendarRequest>()
                service.update(userId, id, request)
                call.respond(HttpMessageResponse("Calendar updated."))
            }

            put("/default") {
                val userId = call.requireUserId()
                val id = call.requirePathUuid("id")
                service.setDefault(userId, id)
                call.respond(HttpMessageResponse("Default calendar updated."))
            }

            put("/position") {
                val userId = call.requireUserId()
                val id = call.requirePathUuid("id")
                val request = call.receive<ReorderCalendarRequest>()
                service.reorder(userId, id, request)
                call.respond(HttpMessageResponse("Position updated."))
            }

            delete {
                val userId = call.requireUserId()
                val request = call.receiveQuery<DeleteCalendarRequest>()
                request.id = call.requirePathUuid("id")

                val result = service.delete(userId, request)
                call.respond(
                    DeleteCalendarResponse(
                        movedItems = result.movedItems,
                        deletedItems = result.deletedItems,
                    )
                )
            }
        }
    }
}
