package app.planer.server.task.data

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp

/** Per-occurrence completion of recurring tasks. */
object TaskCompletionTable : Table("task_completions") {
    val taskId = uuid("task_id")
    val occurrenceDate = date("occurrence_date")
    val completedAt = timestamp("completed_at")

    override val primaryKey = PrimaryKey(taskId, occurrenceDate)
}
