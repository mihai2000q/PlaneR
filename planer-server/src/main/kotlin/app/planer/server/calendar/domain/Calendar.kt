package app.planer.server.calendar.domain

import app.planer.shared.calendar.enums.CalendarIcon
import java.time.Instant
import kotlin.uuid.Uuid

data class Calendar(
    val id: Uuid,
    val userId: Uuid,
    val name: String,
    val color: String,
    val icon: CalendarIcon,
    val position: Int,
    val isDefault: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {

    data class WithItemCount(
        val calendar: Calendar,
        val itemCount: Int,
    )
}
