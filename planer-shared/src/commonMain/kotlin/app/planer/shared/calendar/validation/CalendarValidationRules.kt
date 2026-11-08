package app.planer.shared.calendar.validation

/** Rules that only apply to calendars. Rules shared by several features are in `ValidationRules`. */
object CalendarValidationRules {

    const val NAME_MAX_LENGTH = 40

    /** A calendar name is 1–40 characters once trimmed. */
    fun isValidName(name: String): Boolean = name.trim().length in 1..NAME_MAX_LENGTH
}
