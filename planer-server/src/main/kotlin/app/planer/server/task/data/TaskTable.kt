package app.planer.server.task.data

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp

object TaskTable : UuidTable("tasks") {
    val userId = uuid("user_id")
    val calendarId = uuid("calendar_id").nullable()
    val recurrenceRuleId = uuid("recurrence_rule_id").nullable() // null means "not recurring"
    val title = varchar("title", 120)
    val description = text("description").nullable()
    val dueAt = timestamp("due_at").nullable()
    val scheduledFor = date("scheduled_for").nullable()
    val scheduledStartAt = timestamp("scheduled_start_at").nullable()
    val scheduledEndAt = timestamp("scheduled_end_at").nullable()
    val timeZone = varchar("time_zone", 64)
    val completed = bool("completed").default(false)
    val completedAt = timestamp("completed_at").nullable()
    val position = integer("position").default(0)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
