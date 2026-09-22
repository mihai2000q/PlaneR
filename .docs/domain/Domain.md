# PlaneR Domain

Below is the current state of the domain model for PlaneR.
It is a first pass and may change as the application grows.

## General

Every model has:

- a unique `id` (UUID v7, sortable by creation time)
- `createdAt` and `updatedAt` timestamps (Instant, UTC)

PlaneR is **single-user per account**. There is no sharing, no collaboration, no workspace or team concept. Every model either *is* a User or belongs to one, and every read/write is scoped to the authenticated user.

### Colours and Icons

Where a model supports visual customization, two fields are used:

- **`color`** — a hex string (e.g., `#7C3AED`), rendered as the calendar's accent in the UI
- **`icon`** — a `CalendarIcon` enum value, mapped 1:1 to Material Icons on the client

Both are optional, and the client falls back to a default if either is missing.

---

## Models

### 👤 User

The account holder. Owns every other model in the system.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `email` | string | Unique, used for login, current confirmed address |
| `emailVerified` | bool | Whether the current email has been verified |
| `pendingEmail` | string? | During an email change, the new address awaiting verification |
| `passwordHash` | string | BCrypt |
| `displayName` | string | Shown in the UI |
| `avatarUrl` | string? | Optional |
| `timezone` | string | IANA identifier (e.g., `Europe/Berlin`) |
| `createdAt` | Instant | |
| `updatedAt` | Instant | |

**Notes**

- Email is the identifier. No usernames.
- `timezone` matters because "today" and "this week" are computed in the user's local zone, not the server's.
- A default Calendar is created automatically on registration.
- `emailVerified` starts as `false` on registration. Until it is `true`, the account cannot perform sensitive actions (finalised during design of the verification flow).
- `pendingEmail` is only set during an email change. It is cleared either when the new address is verified (and `email` is updated to match, `emailVerified` reset to `true`) or when the change is cancelled.
- Passwords are **never** stored in plain text — see [Security](../security/Security.md).

### 📅 Calendar

A **user-defined grouping** for events and tasks. This is the equivalent of Google Calendar's sidebar list — a coloured category that items belong to. It is *not* the month grid; the month grid is computed in the UI from events across all calendars.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `userId` | UUID | Owner (FK → User) |
| `name` | string | e.g., "Work" |
| `color` | string | Hex, e.g., `#7C3AED` |
| `icon` | CalendarIcon | Enum, see below |
| `position` | int | Sidebar ordering |
| `isDefault` | bool | Exactly one per user |
| `createdAt` | Instant | |
| `updatedAt` | Instant | |

**Notes**

- "Responsibilities" is just a Calendar whose `name = "Responsibilities"`. No special entity.
- Events and Tasks *may* belong to a Calendar, but they do not have to. A null `calendarId` on those models means "unassigned".

**Default palette**

| Name | Icon | Color |
|---|---|---|
| Work | `BRIEFCASE` | `#7C3AED` |
| Personal | `STAR` | `#F59E0B` |
| Health | `HEALTH` | `#10B981` |
| Family | `FAMILY` | `#EC4899` |
| Responsibilities | `FLAG` | `#EF4444` |

---

### 🗓️ Event

A **time-bound occurrence** on the calendar. Something that happens at a specific time.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `userId` | UUID | Owner (FK → User) |
| `calendarId` | UUID? | Optional grouping (FK → Calendar) |
| `title` | string | |
| `description` | string? | |
| `location` | string? | |
| `startsAt` | Instant | Required |
| `endsAt` | Instant | Required |
| `allDay` | bool | Renders as a banner instead of a block |
| `recurrence` | RecurrenceRule? | See below |
| `createdAt` | Instant | |
| `updatedAt` | Instant | |

**Notes**

- Ownership is via `userId`. When `calendarId` is set, its `userId` must match.
- Events always have a time. When `allDay` is true, the times are still set (start-of-day / end-of-day in the user's timezone), but the UI renders it as a banner.
- No `completed` flag — events happen, they are not "done".

---

### ✅ Task

A **to-do item**. Something the user needs to do and can mark as completed. May or may not have a scheduled time.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `userId` | UUID | Owner (FK → User) |
| `calendarId` | UUID? | Optional grouping (FK → Calendar) |
| `title` | string | |
| `description` | string? | |
| `dueAt` | Instant? | The deadline |
| `scheduledFor` | LocalDate? | The day the user plans to do it |
| `scheduledStartAt` | Instant? | Optional time block on that day |
| `scheduledEndAt` | Instant? | Optional time block on that day |
| `completed` | bool | |
| `completedAt` | Instant? | Set on completion, cleared on uncompletion |
| `position` | int | Per-day ordering |
| `recurrence` | RecurrenceRule? | See below |
| `createdAt` | Instant | |
| `updatedAt` | Instant | |

**Notes**

- Ownership is via `userId`. When `calendarId` is set, its `userId` must match.
- The scheduling fields serve different purposes:
  - `dueAt` — the **deadline** ("must be done by Friday")
  - `scheduledFor` — the **intent** ("I plan to do this Tuesday")
  - `scheduledStartAt` / `scheduledEndAt` — an optional **time block** on that day, rendered on the grid
- This is what powers the UI views:
  - **My Day** → `scheduledFor == today`
  - **This Week** → `scheduledFor` within the current week, or `dueAt` within the week
  - **Backlog** → `scheduledFor == null && dueAt == null`
  - **Completed** → `completed == true`
- A task can be completed without ever being scheduled.
- A Task can be **converted into an Event** when the user decides it has become a scheduled thing rather than a to-do. See "Task ↔ Event Conversion" below.

---

## Task vs Event — quick reference

| | Event | Task |
|---|---|---|
| **Essence** | Something that *happens* | Something you *need to do* |
| **Time** | Always has start & end | Optional |
| **Completion** | No completion state | Has `completed` |
| **Litmus test** | "Does it make sense to mark this done?" → No | → Yes |
| **Example** | "Team Standup, 9:00–9:30" | "Buy groceries" |
| **Example** | "Doctor Appointment, 8:00–9:00" | "Call dentist" |
| **Example** | "Family Brunch, 10:00–12:00" | "Fix leaky faucet" |
| **UI location** | Grid block | Checklist item |

---

## Task ↔ Event Conversion

A Task and an Event represent different intents, but a user's intent can change over time.
A task that has been given a fixed time slot may effectively *become* an event, and vice versa.

PlaneR models this as an explicit **conversion** rather than a link:

- **Task → Event** — the Task is deleted and a new Event is created from its fields.
  The Task's `scheduledStartAt` / `scheduledEndAt` become the Event's `startsAt` / `endsAt`, unless overridden.
- **Event → Task** — the Event is deleted and a new Task is created from its fields.

**Why not link them?** A "task that has become an event" is a conceptual change, not a relationship. Keeping both around would double the surface area of every query and give the UI no clean way to represent a task that is now an event.

This is exposed as two endpoints (documented in `Api.md`):

- `POST /api/tasks/{id}/convert-to-event`
- `POST /api/events/{id}/convert-to-task`

---

## Recurrence

Recurrence is modelled as a **`RecurrenceRule`** value object, shared by Event and Task.
It loosely follows RFC 5545 (iCalendar) but only includes the fields a personal planner actually needs.

### 🔁 RecurrenceRule *(value object)*

| Field | Type | Notes |
|---|---|---|
| `frequency` | Frequency | Daily / Weekly / Monthly / Yearly |
| `interval` | int | Every N (default 1) |
| `byDay` | Set\<DayOfWeek\>? | For Weekly only (e.g., Mon/Wed/Fri) |
| `byMonthDay` | int? | For Monthly only (e.g., 15) |
| `end` | RecurrenceEnd | When the series stops |

### 🛑 RecurrenceEnd *(value object)*

| Field | Type | Notes |
|---|---|---|
| `type` | Never / Until / AfterCount | |
| `untilDate` | Instant? | If Until |
| `count` | int? | If AfterCount |

**How occurrences are produced**

- **Events** — stored once with the rule; the server expands the rule into concrete occurrences when the client requests a date range. One row in the database, many occurrences in the response.
- **Tasks** — the same expansion, but completion is **per occurrence**. Each occurrence that gets completed creates a `TaskCompletion` row (`taskId`, `occurrenceDate`). The UI then marks that specific occurrence as done.

**Exceptions and overrides**

- If the user edits a single occurrence (e.g., moves the July 20 standup to 10am), a **`RecurrenceException`** row is stored that says *"for entity X, on date Y, apply these overrides."* This is the standard iCalendar approach and keeps the recurring entity intact.
- If the user edits an occurrence enough that it should no longer be part of the series, the API splits it into a standalone Event / Task and adds the date to the rule's exception list.

**Presets for the UI** (so the user doesn't type RRULEs)

| Label | Rule |
|---|---|
| Every day | `{ FREQ=DAILY, interval=1 }` |
| Every weekday | `{ FREQ=WEEKLY, byDay=[Mon..Fri] }` |
| Every week on Tuesday | `{ FREQ=WEEKLY, byDay=[Tue] }` |
| Every 2 weeks on Monday | `{ FREQ=WEEKLY, interval=2, byDay=[Mon] }` |
| Every month on the 15th | `{ FREQ=MONTHLY, byMonthDay=15 }` |
| Every year | `{ FREQ=YEARLY }` |
| Custom… | opens the full editor |

**Storage**

`RecurrenceRule` is a value object, so it flattens onto the parent table as columns
(`recurrence_frequency`, `recurrence_interval`, `recurrence_by_day`, `recurrence_until`, `recurrence_count`).
`byDay` becomes a comma-separated string or a small join table, depending on preference.

---

## Enums

### 🎨 CalendarIcon

`BRIEFCASE`, `HEART`, `HOME`, `STAR`, `FLAG`, `BOOK`, `MUSIC`, `FITNESS`, `FOOD`, `TRAVEL`, `MONEY`, `EDUCATION`, `FAMILY`, `HEALTH`, `WORK`, `DEFAULT`

Mapped 1:1 to Material Icons on the client.
The API validates incoming `icon` values against this enum.

**Why an enum, not a free string?**

- Free strings lead to inconsistency ("briefcase", "Briefcase", "work-icon" for the same concept).
- Emoji render differently across platforms (Windows, macOS, Android), which breaks visual consistency in the sidebar.
- An enum maps cleanly to a shared icon set across the **Compose for Desktop** and **Jetpack Compose** targets.

If the user wants a custom look, they can change the **color** instead — that field stays free-form.

### 🔁 Frequency

`DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`

### 🛑 RecurrenceEndType

`NEVER`, `UNTIL`, `AFTER_COUNT`

---

## Derived Concepts

Not stored as their own models — computed from the ones above.

| Concept | Source | Purpose |
|---|---|---|
| **Streak** | `Task.completedAt` grouped by day | Powers the "6 days 🔥" card |
| **Focus Now** | Next upcoming event or scheduled task | Powers the "Focus Now" panel |
| **Overdue** | `Task.dueAt < now && !completed` | Powers the Overdue card |
| **Today's Progress** | Tasks scheduled for today, completed vs. total | Powers the "3 / 7" progress |
| **Calendar counts** | Events + tasks per calendar for the sidebar | Powers the "8", "5", "3" badges |

---

## Deferred

The following are intentionally left out of the initial domain and will be added if needed:

- **Reminders / notifications** — would require a `Reminder` model and a scheduler
- **Attachments** — would require a storage layer
- **DailyCheckIn** — the "Energy Level Today" feature; to be decided
- **Sharing / collaboration** — PlaneR is single-user for now