package app.planer.server.calendar.data

import app.planer.shared.calendar.enums.CalendarIcon
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.javatime.timestamp

object CalendarTable : UuidTable("calendars") {
    val name = varchar("name", 255)
    val color = text("color")
    val icon = enumerationByName("icon", 20, CalendarIcon::class)
    val position = integer("position")
    val isDefault = bool("is_default")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val userId = uuid("user_id")
}
