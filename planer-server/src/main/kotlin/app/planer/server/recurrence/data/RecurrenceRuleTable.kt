package app.planer.server.recurrence.data

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.javatime.date

object RecurrenceRuleTable : UuidTable("recurrence_rules") {
    val frequency = varchar("frequency", 10) // DAILY, WEEKLY, MONTHLY, YEARLY
    val interval = integer("interval").default(1)
    val byDay = varchar("by_day", 20).nullable() // comma-separated ISO day codes, e.g. MO,WE,FR
    val byMonthDay = integer("by_month_day").nullable()
    val endType = varchar("end_type", 16) // NEVER, UNTIL, AFTER_COUNT
    val until = date("until").nullable()
    val count = integer("count").nullable()
}
