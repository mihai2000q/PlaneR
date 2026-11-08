package app.planer.server.auth.data

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.javatime.timestamp

object OneTimeTokenTable : UuidTable("one_time_tokens") {
    val userId = uuid("user_id")
    val purpose = varchar("purpose", 20) // VERIFY_EMAIL, RESET_PASSWORD
    val tokenHash = varchar("token_hash", 64)
    val email = varchar("email", 255)
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at")
    val usedAt = timestamp("used_at").nullable()
}
