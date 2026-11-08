package app.planer.server.task.data

import org.jetbrains.exposed.v1.core.ReferenceOption
import app.planer.server.recurrence.data.RecurrenceRuleTable
import app.planer.server.user.data.UserTable
import app.planer.server.calendar.data.CalendarTable
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp

object TaskTable : UuidTable("tasks") {
    val userId = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE)
    val calendarId = reference("calendar_id", CalendarTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val recurrenceRuleId = reference("recurrence_rule_id", RecurrenceRuleTable, onDelete = ReferenceOption.SET_NULL).nullable() // null means "not recurring"
    val title = varchar("title", 120)
    val description = text("description").nullable()
    val dueAt = timestamp("due_at").nullable()
    val scheduledFor = date("scheduled_for").nullable()
    val scheduledStartAt = timestamp("scheduled_start_at").nullable()
    val scheduledEndAt = timestamp("scheduled_end_at").nullable()
    val timezone = varchar("timezone", 64)
    val completed = bool("completed").default(false)
    val completedAt = timestamp("completed_at").nullable()
    val position = integer("position").default(0)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
