package app.planer.shared.calendar.requests

import app.planer.shared.calendar.enums.CalendarIcon
import kotlinx.serialization.Serializable

@Serializable
data class CreateCalendarRequest(
    val name: String,
    val color: String,
    val icon: CalendarIcon,
)
