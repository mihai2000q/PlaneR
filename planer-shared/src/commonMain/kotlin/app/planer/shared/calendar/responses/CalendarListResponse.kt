package app.planer.shared.calendar.responses

import app.planer.shared.calendar.enums.CalendarIcon
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class CalendarListResponse(
    val calendars: List<CalendarListItemResponse>,
    val totalCount: Int,
)

@Serializable
data class CalendarListItemResponse(
    val id: Uuid,
    val name: String,
    val color: String,
    val icon: CalendarIcon,
    val position: Int,
    val isDefault: Boolean,
    /** Number of events and tasks assigned to the calendar. */
    val itemCount: Int,
)
