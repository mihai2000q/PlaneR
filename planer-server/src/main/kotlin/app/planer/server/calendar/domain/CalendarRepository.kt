package app.planer.server.calendar.domain

import kotlin.uuid.Uuid

interface CalendarRepository {

    /** The highest `position` among the user's calendars, or `null` if the user has none. */
    suspend fun findLastPosition(userId: Uuid): Int?

    /** Stores [calendar] exactly as given. Database failures (such as a unique violation) propagate unchanged. */
    suspend fun create(calendar: Calendar)
}
