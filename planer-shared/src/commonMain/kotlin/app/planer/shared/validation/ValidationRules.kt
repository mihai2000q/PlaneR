package app.planer.shared.validation

object ValidationRules {

    const val TITLE_MAX_LENGTH = 120

    private val hexColor = Regex("^#[0-9A-Fa-f]{6}$")

    /** A title is 1–120 characters once trimmed. */
    fun isValidTitle(title: String): Boolean = title.trim().length in 1..TITLE_MAX_LENGTH

    /** A color is a `#RRGGBB` hex string. */
    fun isValidHexColor(color: String): Boolean = hexColor.matches(color)
}
