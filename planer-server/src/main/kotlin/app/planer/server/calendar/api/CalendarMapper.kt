package app.planer.server.calendar.api

import app.planer.server.calendar.domain.Calendar
import app.planer.shared.calendar.CalendarDto
import kotlin.time.toKotlinInstant

fun Calendar.toDto() = CalendarDto(
    id = id,
    name = name,
    color = color,
    icon = icon,
    position = position,
    isDefault = isDefault,
    createdAt = createdAt.toKotlinInstant(),
    updatedAt = updatedAt.toKotlinInstant(),
)
