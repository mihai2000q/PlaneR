package app.planer.shared.calendar.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What happens to a calendar's events and tasks when it is deleted (`mode` of `DELETE /api/calendars/{id}`). */
@Serializable
enum class CalendarDeleteMode {
    /** Events and tasks keep existing with no calendar. */
    @SerialName("unassign")
    UNASSIGN,

    /** Events and tasks move to another calendar (`targetCalendarId`). */
    @SerialName("reassign")
    REASSIGN,

    /** Events and tasks are deleted along with the calendar. */
    @SerialName("cascade")
    CASCADE,
}
