package app.planer.server.calendar.data

import app.planer.server.user.data.UserTable
import app.planer.server.calendar.domain.Calendar
import app.planer.server.calendar.domain.CalendarRepository
import app.planer.server.common.errors.translatingDatabaseErrors
import app.planer.server.event.data.EventTable
import app.planer.server.task.data.TaskTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.wrapAsExpression
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

class CalendarRepositoryImpl : CalendarRepository {

    override suspend fun findAllByUser(userId: Uuid): List<Calendar.WithItemCount> = withContext(Dispatchers.IO) {
        transaction {
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

    override suspend fun findLastPosition(userId: Uuid): Int? = withContext(Dispatchers.IO) {
        transaction {
            val lastPosition = CalendarTable.position.max()
            CalendarTable
                .select(lastPosition)
                .where { CalendarTable.userId eq userId }
                .single()[lastPosition]
        }
    }

    override suspend fun create(calendar: Calendar) = withContext(Dispatchers.IO) {
        translatingDatabaseErrors {
            transaction {
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
                return@transaction
            }
        }
    }
}
