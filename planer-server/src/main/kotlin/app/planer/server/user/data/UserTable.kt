package app.planer.server.user.data

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.javatime.timestamp

object UserTable : UuidTable("users") {
    val email = varchar("email", 255)
    val emailVerified = bool("email_verified").default(false)
    val pendingEmail = varchar("pending_email", 255).nullable()
    val passwordHash = varchar("password_hash", 255)
    val displayName = varchar("display_name", 255)
    val avatarUrl = varchar("avatar_url", 2048).nullable()
    val timezone = varchar("timezone", 64)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
