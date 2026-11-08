package app.planer.server.calendar.domain

import app.planer.server.common.errors.ConflictException
import app.planer.server.common.errors.NotFoundException
import app.planer.server.common.errors.UniqueViolationException
import app.planer.server.common.errors.ValidationException
import app.planer.server.common.transactions.TransactionRunner
import app.planer.server.common.validation.validateOrThrow
import app.planer.server.event.domain.EventRepository
import app.planer.server.task.domain.TaskRepository
import app.planer.shared.calendar.enums.CalendarDeleteMode
import app.planer.shared.calendar.requests.CreateCalendarRequest
import app.planer.shared.calendar.requests.DeleteCalendarRequest
import app.planer.shared.calendar.requests.ReorderCalendarRequest
import app.planer.shared.calendar.requests.UpdateCalendarRequest
import app.planer.shared.calendar.validation.createCalendarRequestValidation
import app.planer.shared.calendar.validation.deleteCalendarRequestValidation
import app.planer.shared.calendar.validation.reorderCalendarRequestValidation
import app.planer.shared.calendar.validation.updateCalendarRequestValidation
import java.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CalendarService(
    private val repository: CalendarRepository,
    private val eventRepository: EventRepository,
    private val taskRepository: TaskRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock = Clock.systemUTC(),
) {

    suspend fun getList(userId: Uuid): List<Calendar.WithItemCount> = repository.findAllByUser(userId)

    suspend fun get(userId: Uuid, calendarId: Uuid): Calendar =
        repository.findById(userId, calendarId) ?: throw NotFoundException("Calendar not found.")

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

    suspend fun update(userId: Uuid, calendarId: Uuid, request: UpdateCalendarRequest) {
        updateCalendarRequestValidation.validateOrThrow(request)

        val calendar = get(userId, calendarId)
        val updated = calendar.copy(
            name = request.name.trim(),
            color = request.color,
            icon = request.icon,
            updatedAt = clock.instant(),
        )

        try {
            repository.update(updated)
        } catch (e: UniqueViolationException) {
            throw ConflictException(e.message ?: "A calendar with that name already exists.")
        }
    }

    suspend fun setDefault(userId: Uuid, calendarId: Uuid) {
        val calendar = get(userId, calendarId)
        if (calendar.isDefault) return

        repository.setDefault(userId, calendarId, clock.instant())
    }

    /** Moves the calendar to [ReorderCalendarRequest.newPosition]; the calendars in between shift by one. */
    suspend fun reorder(userId: Uuid, calendarId: Uuid, request: ReorderCalendarRequest) {
        reorderCalendarRequestValidation.validateOrThrow(request)

        transactions.run {
            get(userId, calendarId) // 404 if it is not the user's

            val ids = repository.findOrderedIds(userId).toMutableList()
            if (request.newPosition > ids.lastIndex) {
                throw ValidationException(mapOf("newPosition" to listOf("must be between 0 and ${ids.lastIndex}")))
            }

            // Renumbering 0..n-1 also closes any gap left by deleted calendars.
            ids.remove(calendarId)
            ids.add(request.newPosition, calendarId)
            val positions = ids.withIndex().associate { (position, id) -> id to position }

            // Only the calendar that was moved is updated; the others merely shifted to make room.
            repository.updatePositions(positions - calendarId)
            repository.updatePosition(calendarId, positions.getValue(calendarId), clock.instant())
        }
    }

    suspend fun delete(userId: Uuid, request: DeleteCalendarRequest): DeletionResult {
        deleteCalendarRequestValidation.validateOrThrow(request)

        val calendarId = request.id
        val calendar = get(userId, calendarId)
        if (calendar.isDefault) {
            throw ConflictException(
                "the default calendar cannot be deleted; make another calendar the default first"
            )
        }

        // The calendar, its events and its tasks change together, or not at all.
        val result = transactions.run {
            val result = when (request.mode) {
                CalendarDeleteMode.UNASSIGN -> moveItems(calendarId, to = null)
                CalendarDeleteMode.REASSIGN -> moveItems(
                    calendarId,
                    to = requireTarget(userId, request.targetCalendarId!!)
                )

                CalendarDeleteMode.CASCADE -> DeletionResult(
                    movedItems = 0,
                    deletedItems = eventRepository.deleteByCalendar(calendarId) + taskRepository.deleteByCalendar(
                        calendarId
                    ),
                )
            }
            repository.delete(calendarId)
            result
        }
        return result
    }

    private suspend fun moveItems(calendarId: Uuid, to: Uuid?) = DeletionResult(
        movedItems = eventRepository.changeCalendar(calendarId, to) + taskRepository.changeCalendar(calendarId, to),
        deletedItems = 0,
    )

    private suspend fun requireTarget(userId: Uuid, targetCalendarId: Uuid): Uuid {
        if (repository.findById(userId, targetCalendarId) == null) {
            throw NotFoundException("Target calendar not found.")
        }
        return targetCalendarId
    }

    data class DeletionResult(
        /** Items that lost the calendar: reassigned to another one, or left without one. */
        val movedItems: Int,
        val deletedItems: Int,
    )
}
