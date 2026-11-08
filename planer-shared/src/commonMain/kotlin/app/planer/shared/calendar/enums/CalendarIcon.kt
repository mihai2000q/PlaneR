package app.planer.shared.calendar.enums

import kotlinx.serialization.Serializable

/** The icon of a calendar. Mapped 1:1 to Material Icons by the clients */
@Serializable
enum class CalendarIcon {
    BRIEFCASE, HOME, STAR, FLAG, BOOK, MUSIC, FITNESS, FOOD, TRAVEL, MONEY, EDUCATION, FAMILY, HEALTH, DEFAULT
}
