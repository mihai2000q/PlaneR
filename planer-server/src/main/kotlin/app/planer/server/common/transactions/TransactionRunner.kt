package app.planer.server.common.transactions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

/**
 * Runs several repository calls as one unit: all of their changes are committed together, or none are.
 * Repositories called inside [run] join the transaction instead of opening their own.
 */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

class ExposedTransactionRunner : TransactionRunner {
    // Repositories open their own `suspendTransaction`; inside this one they join it through the coroutine context.
    override suspend fun <T> run(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        suspendTransaction { block() }
    }
}
