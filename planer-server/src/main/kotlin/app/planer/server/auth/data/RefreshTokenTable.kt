package app.planer.server.auth.data

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.javatime.timestamp

object RefreshTokenTable : UuidTable("refresh_tokens") {
    val userId = uuid("user_id")
    val familyId = uuid("family_id")
    val tokenHash = varchar("token_hash", 64)
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at")
    val revokedAt = timestamp("revoked_at").nullable()
}
