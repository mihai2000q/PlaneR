package app.planer.server.task.data

import app.planer.server.calendar.data.CalendarTable
import app.planer.server.recurrence.data.RecurrenceRuleTable
import app.planer.server.task.domain.TaskRepository
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

class TaskRepositoryImpl : TaskRepository {

    override suspend fun changeCalendar(from: Uuid, to: Uuid?): Int = withContext(Dispatchers.IO) {
        suspendTransaction {
            val target = to?.let { EntityID(it, CalendarTable) }
            TaskTable.update({ TaskTable.calendarId eq from }) { it[TaskTable.calendarId] = target }
        }
    }

    override suspend fun deleteByCalendar(calendarId: Uuid): Int = withContext(Dispatchers.IO) {
        suspendTransaction {
            // A recurrence rule belongs to one task and nothing cascades to it, so it is deleted explicitly.
            val ruleIds = TaskTable
                .select(TaskTable.recurrenceRuleId)
                .where { (TaskTable.calendarId eq calendarId) and TaskTable.recurrenceRuleId.isNotNull() }
                .map { it[TaskTable.recurrenceRuleId]!!.value }

            val deleted = TaskTable.deleteWhere { TaskTable.calendarId eq calendarId }
            if (ruleIds.isNotEmpty()) {
                RecurrenceRuleTable.deleteWhere { RecurrenceRuleTable.id inList ruleIds }
            }
            deleted
        }
    }
}
