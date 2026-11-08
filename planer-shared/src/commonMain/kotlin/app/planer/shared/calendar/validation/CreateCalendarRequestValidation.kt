package app.planer.shared.calendar.validation

import app.planer.shared.calendar.requests.CreateCalendarRequest
import io.konform.validation.Validation
import app.planer.shared.validation.ValidationRules

val createCalendarRequestValidation = Validation {
    CreateCalendarRequest::name {
        constrain("must be 1-${CalendarValidationRules.NAME_MAX_LENGTH} characters") { CalendarValidationRules.isValidName(it) }
    }
    CreateCalendarRequest::color {
        constrain("must be a #RRGGBB hex color") { ValidationRules.isValidHexColor(it) }
    }
}
