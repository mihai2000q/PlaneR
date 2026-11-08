package app.planer.server.task.domain

import kotlin.uuid.Uuid

interface TaskRepository {

    /**
     * Moves every task of calendar [from] to calendar [to], or leaves them without a calendar when [to] is `null`.
     * Returns how many tasks moved.
     */
    suspend fun changeCalendar(from: Uuid, to: Uuid?): Int

    /** Deletes every task of the calendar, together with their recurrence rules. Returns how many were deleted. */
    suspend fun deleteByCalendar(calendarId: Uuid): Int
}
