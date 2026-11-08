package app.planer.server.calendar.domain

interface CalendarRepository {

    suspend fun create(calendar: Calendar): Calendar
}
