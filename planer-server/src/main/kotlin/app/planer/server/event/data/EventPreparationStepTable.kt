package app.planer.server.event.data

import org.jetbrains.exposed.v1.core.dao.id.UuidTable

/** An event's routine. The id is a storage-only surrogate key, never exposed by the API. */
object EventPreparationStepTable : UuidTable("event_preparation_steps") {
    val eventId = uuid("event_id")
    val name = varchar("name", 255)
    val duration = integer("duration") // minutes
    val position = integer("position") // 0-based order in the sequence
}
