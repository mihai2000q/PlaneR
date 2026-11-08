package app.planer.server.calendar.data

import app.planer.server.calendar.domain.Calendar
import app.planer.server.calendar.domain.CalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.Uuid

class CalendarRepositoryImpl : CalendarRepository {

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
        transaction {
            CalendarEntity.new(calendar.id) {
                userId = calendar.userId
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
