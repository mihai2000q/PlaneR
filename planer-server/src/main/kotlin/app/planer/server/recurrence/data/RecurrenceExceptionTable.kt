package app.planer.server.recurrence.data

import org.jetbrains.exposed.v1.core.ReferenceOption
import app.planer.server.task.data.TaskTable
import app.planer.server.event.data.EventTable
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * Overrides and skips of single occurrences. Exactly one of [eventId] / [taskId] is set (enforced by the server).
 * NULL in an override column means "not overridden".
 *
 * The `routine_override` jsonb column is not mapped yet: it needs the `exposed-json` module.
 */
object RecurrenceExceptionTable : UuidTable("recurrence_exceptions") {
    val eventId = reference("event_id", EventTable, onDelete = ReferenceOption.CASCADE).nullable()
    val taskId = reference("task_id", TaskTable, onDelete = ReferenceOption.CASCADE).nullable()
    val occurrenceDate = date("occurrence_date")
    val skipped = bool("skipped").default(false)

    // Overrides
    val title = varchar("title", 120).nullable()
    val description = text("description").nullable()

    // Event-only overrides
    val location = varchar("location", 255).nullable()
    val startsAt = timestamp("starts_at").nullable()
    val endsAt = timestamp("ends_at").nullable()
    val allDay = bool("all_day").nullable()

    // Task-only overrides
    val dueAt = timestamp("due_at").nullable()
    val scheduledFor = date("scheduled_for").nullable()
    val scheduledStartAt = timestamp("scheduled_start_at").nullable()
    val scheduledEndAt = timestamp("scheduled_end_at").nullable()
}
