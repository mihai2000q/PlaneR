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
Something that *happens* at a specific time. Always has a start and an end. Has no completion state. May carry a **routine** — an ordered list of preparation steps — for reverse planning. Example: "Meet friends, 17:30–20:00, with a routine starting at 15:30".

**Task**
Something the user *needs to do*. May or may not have a time. Has a completion state. Never carries a routine. Example: "Buy groceries".

**Routine**
The ordered list of **preparation steps** attached to an Event. Used for reverse planning — the server lays the steps out backward from the event's `startsAt` so the user knows when to begin preparing. A Task never has a routine.

**Preparation Step**
A single step within a routine. Defined by a `name` (e.g., "Shower") and a `duration` in minutes. Sequential — one step ends exactly when the next begins. Has no ID and no completion state; it lives and dies with its event.

**RecurrenceRule**
A value object that describes how an event or task repeats (daily, weekly, monthly, yearly). Defined by a frequency, interval, optional day-of-week set, and an end condition.

**RecurrenceEnd**
A value object describing when a recurring series stops. One of: never, on a specific date, or after N occurrences.

**CalendarIcon**
An enum of icon identifiers for calendars (`BRIEFCASE`, `HEALTH`, `FITNESS`, …). Mapped to Material Icons on the client.

**Unassigned**
An event or task whose `calendarId` is null. It exists but is not grouped under any calendar.

## Time and Views

**Timezone**
Two related things. `User.timezone` (an IANA identifier such as `Europe/Berlin`) is authoritative for "today", "this week", and date-range filters, and is the default for new items. Each event and task also has its own `timeZone`, which anchors recurrence and all-day boundaries so that existing items don't move when the user's timezone changes. The server never assumes a timezone. See [Domain → Timezones](Domain.md#timezones).

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

**Routine Start**
The moment an event's first preparation step begins. Computed as `Event.startsAt - sum(routine.duration)`. Powers the "start preparing at 3:30 PM" card and the faded prep block on the calendar grid.

**Today's Progress**
The count of tasks scheduled for today that are completed, versus the total. Powers the "3 / 7" progress indicator.

**Item Count**
The number of events and tasks assigned to a calendar. Powers the small number beside each calendar in the sidebar.

## Technical

**KMP**
Kotlin Multiplatform. The mechanism that lets the same Kotlin code compile to JVM, Android, and iOS targets. The **Shared** module is the only KMP module.

**Jetpack Compose**
Android's declarative UI toolkit. Used by the Android client.

**Compose for Desktop**
The JVM desktop flavour of Compose. Used by the Desktop client. It uses the same toolkit family as Android, but the two clients share no UI code.

**Client**
One of PlaneR's apps: Desktop, Android, and later iOS. Each owns its screens, navigation, and view models; none shares UI code with another. Android (and a future iOS client) are native apps. Desktop runs on the JVM with Compose for Desktop, which draws its own UI rather than using the operating system's widgets, so it is cross-platform, not native.

**Shared**
The shared library module (`planer-shared`). Contains domain models, DTOs, validation, and the networking client. Has no UI. The clients depend on all of it; the server depends only on the models, DTOs, and validation.

**Forced Update**
The mechanism by which the server refuses requests from client versions below a configured minimum, answering `426 Upgrade Required`. Clients announce their version with the `X-Client-Version` and `X-Client-Platform` headers.

**ApiError**
The typed failure value that `ApiClient` returns for every non-2xx response. See [Error Handling](../architecture/Error-Handling.md).

**Server**
The Ktor HTTP server module (`planer-server`). Owns persistence and business logic.

**JWT**
JSON Web Token. The format of the 15-minute access token. Contains a `sub` claim with the user ID and is signed with HS256.

**Refresh Token**
A long-lived (60-day), opaque, rotating token used to obtain new access tokens without re-login. Stored hashed in the database so it can be revoked.

**One-Time Token**
An opaque, single-use, hashed-at-rest token used for email verification and password reset. Not a JWT.

**RRULE**
The iCalendar standard notation for recurrence rules. PlaneR's `RecurrenceRule` is loosely modelled after it but does not use the string syntax.

**Flyway**
The database migration tool. Applies versioned SQL files at startup.

**Ktor**
The Kotlin HTTP framework used for both the server and the client networking layer.

**Exposed**
JetBrains' Kotlin SQL framework. The persistence layer of the server.

**DTO**
Data Transfer Object. The shape of a request or response body, distinct from the domain model.