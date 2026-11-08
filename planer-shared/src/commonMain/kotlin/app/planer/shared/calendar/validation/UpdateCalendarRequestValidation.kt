package app.planer.shared.calendar.validation

import app.planer.shared.calendar.requests.UpdateCalendarRequest
import app.planer.shared.validation.ValidationRules
import io.konform.validation.Validation

val updateCalendarRequestValidation = Validation {
    UpdateCalendarRequest::name {
        constrain("must be 1-${CalendarValidationRules.NAME_MAX_LENGTH} characters") { CalendarValidationRules.isValidName(it) }
    }
    UpdateCalendarRequest::color {
        constrain("must be a #RRGGBB hex color") { ValidationRules.isValidHexColor(it) }
    }
}
