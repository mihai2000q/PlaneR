# PlaneR Glossary

Terms used throughout the documentation and the codebase.
Where a term has a precise meaning in the app, that meaning is stated here —
it may differ from how the word is used elsewhere.

- [Domain](#domain)
- [Time and Views](#time-and-views)
- [Derived Concepts](#derived-concepts)
- [Technical](#technical)

## Domain

**User**
The account holder. Owns every other entity. Identified by email. Has a timezone that determines what "today" means for them.

**Calendar**
A user-defined, coloured grouping for events and tasks. Equivalent to a list in the sidebar. Not to be confused with the month view.

**Event**
Something that *happens* at a specific time. Always has a start and an end. Has no completion state. Example: "Team Standup, 9:00–9:30".

**Task**
Something the user *needs to do*. May or may not have a time. Has a completion state. Example: "Buy groceries".

**RecurrenceRule**
A value object that describes how an event or task repeats (daily, weekly, monthly, yearly). Defined by a frequency, interval, optional day-of-week set, and an end condition.

**RecurrenceEnd**
A value object describing when a recurring series stops. One of: never, on a specific date, or after N occurrences.

**CalendarIcon**
An enum of icon identifiers for calendars (`BRIEFCASE`, `HEART`, `FITNESS`, …). Mapped to Material Icons on the client.

**Unassigned**
An event or task whose `calendarId` is null. It exists but is not grouped under any calendar.

## Time and Views

**Timezone**
The `User.timezone` field, an IANA identifier (e.g., `Europe/Berlin`). It is authoritative for computing "today", "this week", and recurrence occurrences. The server never assumes a timezone.

**Today**
The current date in the user's timezone.

**This Week**
The seven days containing today, starting on the user's preferred first day of the week (Sunday or Monday).

**My Day**
The view showing tasks scheduled for today. Driven by `Task.scheduledFor == today`.

**Backlog**
Tasks with no schedule and no due date (`scheduledFor == null && dueAt == null`). They exist but are not on the user's radar until they are scheduled.

**Completed**
Tasks where `completed == true`. Kept around for history and streak computation.

**Overdue**
Tasks where `dueAt < now && !completed`. Surfaced separately in the UI.

## Derived Concepts

**Streak**
The number of consecutive days on which at least one task was completed. Computed from `Task.completedAt`, grouped by day in the user's timezone. Powers the "6 days 🔥" card.

**Focus Now**
The next upcoming event or scheduled task. Powers the "Focus Now" panel. Computed from `Event.startsAt` and `Task.scheduledStartAt`.

**Today's Progress**
The count of tasks scheduled for today that are completed, versus the total. Powers the "3 / 7" progress indicator.

**Item Count**
The number of events and tasks assigned to a calendar. Powers the small number beside each calendar in the sidebar.

## Technical

**KMP**
Kotlin Multiplatform. The mechanism that lets the same Kotlin code compile to JVM, Android, and iOS targets. The **Core** module is a KMP module.

**Compose Multiplatform**
The umbrella framework that provides a shared declarative UI across platforms. On Android it uses **Jetpack Compose**; on Desktop it uses **Compose for Desktop**.

**Core**
The shared library module (`planer-core`). Contains domain models, DTOs, validation, networking, and shared UI. Every other module depends on it.

**Server**
The Ktor HTTP server module (`planer-server`). Owns persistence and business logic.

**JWT**
JSON Web Token. The authentication mechanism. Contains a `sub` claim with the user ID and is signed with HS256.

**Refresh Token**
A long-lived token used to obtain new access tokens without re-login. Stored in the database so it can be revoked.

**RRULE**
The iCalendar standard notation for recurrence rules. PlaneR's `RecurrenceRule` is loosely modelled after it but does not use the string syntax.

**Flyway**
The database migration tool. Applies versioned SQL files at startup.

**Ktor**
The Kotlin HTTP framework used for both the server and the client networking layer.

**Exposed**
A Kotlin SQL framework. Under consideration as the persistence layer, not yet decided.

**DTO**
Data Transfer Object. The shape of a request or response body, distinct from the domain model.