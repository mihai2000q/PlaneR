package app.planer.server.calendar.data

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

class CalendarEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<CalendarEntity>(CalendarTable)

    var userId by CalendarTable.userId
    var name by CalendarTable.name
    var color by CalendarTable.color
    var icon by CalendarTable.icon
    var position by CalendarTable.position
    var isDefault by CalendarTable.isDefault
    var createdAt by CalendarTable.createdAt
    var updatedAt by CalendarTable.updatedAt
}
