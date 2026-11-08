package app.planer.server.common.errors

import org.postgresql.util.PSQLException
import java.sql.SQLException

/** PostgreSQL SQLSTATE codes the server reacts to. */
object SqlState {
    /** `unique_violation`: a unique constraint or index rejected the write. */
    const val UNIQUE_VIOLATION = "23505"
}

/**
 * A database failure the server knows how to name, translated from the SQL error so that nothing above the data layer
 * deals with SQL types or SQLSTATE codes. The mapping lives in [toDatabaseException].
 */
sealed class DatabaseException(message: String, cause: Throwable) : RuntimeException(message, cause)

/** A unique constraint or index rejected a write. [table] and [constraint] are read from the database's own error. */
class UniqueViolationException(val table: String?, val constraint: String?, cause: Throwable) : DatabaseException(
    "Unique constraint '${constraint ?: "unknown"}' on table '${table ?: "unknown"}' was violated.",
    cause,
)

/**
 * Maps this SQL error to a [DatabaseException], or returns `null` when the server has no name for it (the caller then
 * rethrows the original). The table and constraint come from the PostgreSQL error nested in the exception chain.
 */
fun SQLException.toDatabaseException(): DatabaseException? {
    val postgres = generateSequence<Throwable>(this) { it.cause }.filterIsInstance<PSQLException>().firstOrNull()
    val server = postgres?.serverErrorMessage
    return when (postgres?.sqlState ?: sqlState) {
        SqlState.UNIQUE_VIOLATION -> UniqueViolationException(server?.table, server?.constraint, this)
        else -> null
    }
}

/** Runs [block] and translates the SQL errors the server knows into [DatabaseException]s; others pass through. */
inline fun <T> translatingDatabaseErrors(block: () -> T): T =
    try {
        block()
    } catch (e: SQLException) {
        throw e.toDatabaseException() ?: e
    }
