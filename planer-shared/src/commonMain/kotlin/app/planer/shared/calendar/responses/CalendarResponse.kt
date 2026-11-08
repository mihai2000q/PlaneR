package app.planer.shared.calendar.responses

import app.planer.shared.calendar.enums.CalendarIcon
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class CalendarResponse(
    val id: Uuid,
    val name: String,
    val color: String,
    val icon: CalendarIcon,
    val position: Int,
    val isDefault: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
