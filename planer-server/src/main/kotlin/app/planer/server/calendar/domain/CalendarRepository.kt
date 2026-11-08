package app.planer.server.calendar.domain

import kotlin.uuid.Uuid

interface CalendarRepository {

    /** The highest `position` among the user's calendars, or `null` if the user has none. */
    suspend fun findLastPosition(userId: Uuid): Int?

    /**
     * Stores [calendar] exactly as given.
     *
     * @throws app.planer.server.common.errors.UniqueViolationException if a unique constraint rejects the write
     */
    suspend fun create(calendar: Calendar)
}
