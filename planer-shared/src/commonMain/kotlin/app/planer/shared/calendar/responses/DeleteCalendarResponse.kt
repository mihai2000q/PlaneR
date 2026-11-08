package app.planer.shared.calendar.responses

import kotlinx.serialization.Serializable

@Serializable
data class DeleteCalendarResponse(
    /** Events and tasks that lost this calendar: reassigned to another one, or left without one. */
    val movedItems: Int,
    /** Events and tasks deleted along with the calendar (`mode=cascade`). */
    val deletedItems: Int,
)
