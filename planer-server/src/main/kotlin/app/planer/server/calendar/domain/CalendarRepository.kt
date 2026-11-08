package app.planer.server.calendar.domain

import kotlin.uuid.Uuid

interface CalendarRepository {

    /** All of the user's calendars with their item counts, ordered by `position`. */
    suspend fun findAllByUser(userId: Uuid): List<Calendar.WithItemCount>

    /** The highest `position` among the user's calendars, or `null` if the user has none. */
    suspend fun findLastPosition(userId: Uuid): Int?

    /**
     * Stores [calendar] exactly as given.
     *
     * @throws app.planer.server.common.errors.UniqueViolationException if a unique constraint rejects the write
     */
    suspend fun create(calendar: Calendar)
}
