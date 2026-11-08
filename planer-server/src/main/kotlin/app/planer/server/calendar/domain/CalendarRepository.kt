package app.planer.server.calendar.domain

import java.time.Instant
import kotlin.uuid.Uuid

interface CalendarRepository {

    /** All of the user's calendars with their item counts, ordered by `position`. */
    suspend fun findAllByUser(userId: Uuid): List<Calendar.WithItemCount>

    /** The user's calendar with this id, or `null` if there is none (including when it belongs to someone else). */
    suspend fun findById(userId: Uuid, calendarId: Uuid): Calendar?

    /** The highest `position` among the user's calendars, or `null` if the user has none. */
    suspend fun findLastPosition(userId: Uuid): Int?

    /**
     * Stores [calendar] exactly as given.
     *
     * @throws app.planer.server.common.errors.UniqueViolationException if a unique constraint rejects the write
     */
    suspend fun create(calendar: Calendar)

    /**
     * Overwrites the name, color, icon and `updatedAt` of the stored calendar with [calendar]'s.
     *
     * @throws app.planer.server.common.errors.UniqueViolationException if a unique constraint rejects the write
     */
    suspend fun update(calendar: Calendar)

    /** Makes the calendar the user's only default, in one transaction. */
    suspend fun setDefault(userId: Uuid, calendarId: Uuid, updatedAt: Instant)

    /** Deletes the calendar. What happens to its events and tasks is decided by the caller. */
    suspend fun delete(calendarId: Uuid)
}
