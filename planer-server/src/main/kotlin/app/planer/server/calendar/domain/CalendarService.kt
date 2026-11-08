package app.planer.server.calendar.domain

import app.planer.server.common.errors.ConflictException
import app.planer.server.common.errors.UniqueViolationException
import app.planer.server.common.validation.validateOrThrow
import app.planer.shared.calendar.requests.CreateCalendarRequest
import app.planer.shared.calendar.validation.createCalendarRequestValidation
import java.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CalendarService(
    private val repository: CalendarRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    @OptIn(ExperimentalUuidApi::class)
    suspend fun create(userId: Uuid, request: CreateCalendarRequest) {
        createCalendarRequestValidation.validateOrThrow(request)

        // New calendars are appended at the end
        val position = (repository.findLastPosition(userId) ?: -1) + 1

        val now = clock.instant()
        val calendar = Calendar(
            id = Uuid.generateV7(),
            userId = userId,
            name = request.name.trim(),
            color = request.color,
            icon = request.icon,
            position = position,
            isDefault = false,
            createdAt = now,
            updatedAt = now,
        )

        try {
            repository.create(calendar)
        } catch (e: UniqueViolationException) {
            throw ConflictException(e.message ?: "A calendar with that name already exists.")
        }
    }
}
