package app.planer.shared.calendar.validation

import app.planer.shared.calendar.requests.ReorderCalendarRequest
import io.konform.validation.Validation

val reorderCalendarRequestValidation = Validation {
    ReorderCalendarRequest::newPosition {
        constrain("must be 0 or greater") { it >= 0 }
    }
}
