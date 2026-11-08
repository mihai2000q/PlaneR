package app.planer.shared.calendar.validation

import app.planer.shared.calendar.enums.CalendarDeleteMode
import app.planer.shared.calendar.requests.DeleteCalendarRequest
import io.konform.validation.Validation

/**
 * The rules of `DELETE /api/calendars/{id}`: when `mode` is `reassign`, `targetCalendarId` is required and must be
 * another calendar than the one being deleted.
 */
val deleteCalendarRequestValidation = Validation<DeleteCalendarRequest> {
    dynamic { request ->
        if (request.mode == CalendarDeleteMode.REASSIGN) {
            DeleteCalendarRequest::targetCalendarId {
                constrain("is required when mode is reassign") { it != null }
                constrain("must be another calendar than the one being deleted") { it != request.id }
            }
        }
    }
}
