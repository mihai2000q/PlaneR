package app.planer.server.event.domain

import kotlin.uuid.Uuid

interface EventRepository {

    /**
     * Moves every event of calendar [from] to calendar [to], or leaves them without a calendar when [to] is `null`.
     * Returns how many events moved.
     */
    suspend fun changeCalendar(from: Uuid, to: Uuid?): Int

    /** Deletes every event of the calendar, together with their recurrence rules. Returns how many were deleted. */
    suspend fun deleteByCalendar(calendarId: Uuid): Int
}
