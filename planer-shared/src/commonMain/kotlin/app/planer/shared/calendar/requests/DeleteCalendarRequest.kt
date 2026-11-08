package app.planer.shared.calendar.requests

import app.planer.shared.calendar.enums.CalendarDeleteMode
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/** The query parameters of `DELETE /api/calendars/{id}`, plus the calendar's [id] from the path. */
@Serializable
data class DeleteCalendarRequest(
    val mode: CalendarDeleteMode = CalendarDeleteMode.UNASSIGN,
    val targetCalendarId: Uuid? = null,
) {
    /** Not a query parameter: the route sets it from the path before the request is validated. */
    var id: Uuid = Uuid.NIL
}
