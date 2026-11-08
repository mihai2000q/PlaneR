package app.planer.server.event.data

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.javatime.timestamp

object EventTable : UuidTable("events") {
    val userId = uuid("user_id")
    val calendarId = uuid("calendar_id").nullable()
    val recurrenceRuleId = uuid("recurrence_rule_id").nullable() // null means "not recurring"
    val title = varchar("title", 120)
    val description = text("description").nullable()
    val location = varchar("location", 255).nullable()
    val startsAt = timestamp("starts_at")
    val endsAt = timestamp("ends_at")
    val timezone = varchar("timezone", 64)
    val allDay = bool("all_day").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
