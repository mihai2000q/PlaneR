package app.planer.shared.calendar.validation

import app.planer.shared.calendar.requests.CreateCalendarRequest
import io.konform.validation.Validation
import app.planer.shared.validation.ValidationRules

/**
 * The rules of `POST /api/calendars` (see `Api.Calendar.md`). Run it on the raw request: the name rule trims before
 * measuring. Clients can use it for instant feedback, and the server applies it as the source of truth.
 */
val createCalendarRequestValidation = Validation {
    CreateCalendarRequest::name {
        constrain("must be 1-${CalendarValidationRules.NAME_MAX_LENGTH} characters") { CalendarValidationRules.isValidName(it) }
    }
    CreateCalendarRequest::color {
        constrain("must be a #RRGGBB hex color") { ValidationRules.isValidHexColor(it) }
    }
}
