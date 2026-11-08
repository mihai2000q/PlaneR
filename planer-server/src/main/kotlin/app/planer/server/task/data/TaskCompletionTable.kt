package app.planer.server.task.data

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp

/** Per-occurrence completion of recurring tasks. */
object TaskCompletionTable : Table("task_completions") {
    val taskId = reference("task_id", TaskTable, onDelete = ReferenceOption.CASCADE)
    val occurrenceDate = date("occurrence_date")
    val completedAt = timestamp("completed_at")

    override val primaryKey = PrimaryKey(taskId, occurrenceDate)
}
