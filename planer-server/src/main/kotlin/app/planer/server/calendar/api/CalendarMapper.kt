package app.planer.server.calendar.api

import app.planer.server.calendar.domain.Calendar
import app.planer.shared.calendar.responses.CalendarResponse
import app.planer.shared.calendar.responses.CalendarListItemResponse
import app.planer.shared.calendar.responses.CalendarListResponse
import kotlin.time.toKotlinInstant

fun List<Calendar.WithItemCount>.toListResponse() = CalendarListResponse(
    calendars = map { it.calendar.toListItemResponse(it.itemCount) },
    totalCount = size,
)

private fun Calendar.toListItemResponse(itemCount: Int) = CalendarListItemResponse(
    id = id,
    name = name,
    color = color,
    icon = icon,
    position = position,
    isDefault = isDefault,
    itemCount = itemCount,
)

fun Calendar.toResponse() = CalendarResponse(
    id = id,
    name = name,
    color = color,
    icon = icon,
    position = position,
    isDefault = isDefault,
    createdAt = createdAt.toKotlinInstant(),
    updatedAt = updatedAt.toKotlinInstant(),
)
