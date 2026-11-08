package app.planer.server.event.data

import app.planer.server.calendar.data.CalendarTable
import app.planer.server.event.domain.EventRepository
import app.planer.server.recurrence.data.RecurrenceRuleTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.Uuid

class EventRepositoryImpl : EventRepository {

    override suspend fun changeCalendar(from: Uuid, to: Uuid?): Int = withContext(Dispatchers.IO) {
        suspendTransaction {
            val target = to?.let { EntityID(it, CalendarTable) }
            EventTable.update({ EventTable.calendarId eq from }) { it[EventTable.calendarId] = target }
        }
    }

    override suspend fun deleteByCalendar(calendarId: Uuid): Int = withContext(Dispatchers.IO) {
        suspendTransaction {
            // A recurrence rule belongs to one event and nothing cascades to it, so it is deleted explicitly.
            val ruleIds = EventTable
                .select(EventTable.recurrenceRuleId)
                .where { (EventTable.calendarId eq calendarId) and EventTable.recurrenceRuleId.isNotNull() }
                .map { it[EventTable.recurrenceRuleId]!!.value }

            val deleted = EventTable.deleteWhere { EventTable.calendarId eq calendarId }
            if (ruleIds.isNotEmpty()) {
                RecurrenceRuleTable.deleteWhere { RecurrenceRuleTable.id inList ruleIds }
            }
            deleted
        }
    }
}
