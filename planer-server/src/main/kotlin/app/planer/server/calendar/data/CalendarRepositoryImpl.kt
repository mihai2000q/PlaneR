package app.planer.server.calendar.data

import app.planer.server.user.data.UserTable
import app.planer.server.calendar.domain.Calendar
import app.planer.server.calendar.domain.CalendarRepository
import app.planer.server.common.errors.translatingDatabaseErrors
import app.planer.server.event.data.EventTable
import app.planer.server.task.data.TaskTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.wrapAsExpression
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import kotlin.uuid.Uuid

class CalendarRepositoryImpl : CalendarRepository {

    override suspend fun findAllByUser(userId: Uuid): List<Calendar.WithItemCount> = withContext(Dispatchers.IO) {
        suspendTransaction {
            // One query: the counts are scalar subqueries next to the calendar columns.
            val eventCount = wrapAsExpression<Long>(
                EventTable
                    .select(EventTable.id.count())
                    .where { EventTable.calendarId eq CalendarTable.id }
            )
            val taskCount = wrapAsExpression<Long>(
                TaskTable
                    .select(TaskTable.id.count())
                    .where { TaskTable.calendarId eq CalendarTable.id }
            )

            CalendarTable
                .select(CalendarTable.columns + eventCount + taskCount)
                .where { CalendarTable.userId eq userId }
                .orderBy(CalendarTable.position)
                .map { row ->
                    Calendar.WithItemCount(
                        calendar = CalendarEntity.wrapRow(row).toDomain(),
                        itemCount = ((row[eventCount] ?: 0L) + (row[taskCount] ?: 0L)).toInt(),
                    )
                }
        }
    }

    override suspend fun findById(userId: Uuid, calendarId: Uuid): Calendar? = withContext(Dispatchers.IO) {
        suspendTransaction {
            CalendarEntity
                .find { (CalendarTable.id eq calendarId) and (CalendarTable.userId eq userId) }
                .singleOrNull()
                ?.toDomain()
        }
    }

    override suspend fun update(calendar: Calendar) = withContext(Dispatchers.IO) {
        translatingDatabaseErrors {
            suspendTransaction {
                CalendarEntity.findById(calendar.id)?.let {
                    it.name = calendar.name
                    it.color = calendar.color
                    it.icon = calendar.icon
                    it.updatedAt = calendar.updatedAt
                }
                return@suspendTransaction
            }
        }
    }

    override suspend fun setDefault(userId: Uuid, calendarId: Uuid, updatedAt: Instant) = withContext(Dispatchers.IO) {
        suspendTransaction {
            // Unset first: a unique index allows at most one default per user.
            CalendarTable.update({ (CalendarTable.userId eq userId) and (CalendarTable.isDefault eq true) }) {
                it[CalendarTable.isDefault] = false
                it[CalendarTable.updatedAt] = updatedAt
            }
            CalendarTable.update({ CalendarTable.id eq calendarId }) {
                it[CalendarTable.isDefault] = true
                it[CalendarTable.updatedAt] = updatedAt
            }
            return@suspendTransaction
        }
    }

    override suspend fun delete(calendarId: Uuid) = withContext(Dispatchers.IO) {
        suspendTransaction {
            CalendarTable.deleteWhere { CalendarTable.id eq calendarId }
            return@suspendTransaction
        }
    }

    override suspend fun findLastPosition(userId: Uuid): Int? = withContext(Dispatchers.IO) {
        suspendTransaction {
            val lastPosition = CalendarTable.position.max()
            CalendarTable
                .select(lastPosition)
                .where { CalendarTable.userId eq userId }
                .single()[lastPosition]
        }
    }

    override suspend fun create(calendar: Calendar) = withContext(Dispatchers.IO) {
        translatingDatabaseErrors {
            suspendTransaction {
                CalendarEntity.new(calendar.id) {
                    userId = EntityID(calendar.userId, UserTable)
                    name = calendar.name
                    color = calendar.color
                    icon = calendar.icon
                    position = calendar.position
                    isDefault = calendar.isDefault
                    createdAt = calendar.createdAt
                    updatedAt = calendar.updatedAt
                }
                return@suspendTransaction
            }
        }
    }
}
