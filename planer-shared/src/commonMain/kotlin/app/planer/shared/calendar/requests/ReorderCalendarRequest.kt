package app.planer.shared.calendar.requests

import kotlinx.serialization.Serializable

@Serializable
data class ReorderCalendarRequest(
    val newPosition: Int,
)
