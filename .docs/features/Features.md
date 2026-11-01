# PlaneR Features

This document lists what PlaneR does, in the current release.
It is split into **Implemented**, **Planned**, and **Deferred** so that readers
know what is real and what is an aspiration.

- [Implemented](#implemented)
- [Planned](#planned)
  - [Routine Start Reminder](#routine-start-reminder)
- [Deferred](#deferred)
- [Explicitly Out of Scope](#explicitly-out-of-scope)

## Implemented

> No user-facing features yet. The repository contains a building skeleton of all four
> modules: a Gradle build, a shared module with a health DTO and two validation rules, a
> Ktor server serving `GET /health`, and a desktop and an Android app that each show a
> placeholder screen. Everything below is design; this section will be populated as
> features land.

## Planned

Features that are designed and will be built. The domain and API are already
shaped to support them.

### Accounts

- **Registration** with email and password
- **Login** with JWT access and refresh tokens
- **Email verification** before sensitive actions are allowed
- **Password reset** via email link
- **Change email** with re-verification of the new address, a notice to the old address, and the option to cancel or resend
- **Change password** with sign-out of all other sessions
- **Sessions that renew themselves** — short-lived access tokens plus rotating refresh tokens, with theft detection
- **Account deletion** with cascading removal of all owned data

### Calendars

- **Multiple calendars** per user, each with a name, color, and icon
- **Default calendar** created on registration
- **Reorder** calendars in the sidebar
- **Change the default** calendar
- **Delete** a calendar with options for its contents (unassign, reassign, cascade)

### Events

- **Create, edit, and delete** events with title, description, location, start, and end
- **All-day events** rendered as banners instead of time blocks
- **Recurring events** with daily, weekly, monthly, and yearly frequencies
- **Occurrence overrides** for editing a single instance of a recurring event
- **"This and following"** edits that split a series
- **Skip occurrences** without breaking the series
- **Per-event timezone**, so recurring events stay at the same local time across daylight-saving changes and don't move when the user changes timezone
- **Routines** — an ordered list of preparation steps performed before the event starts, used for reverse planning. The server computes when each step begins so the user knows when to start getting ready.

### Tasks

- **Create, edit, and delete** tasks with title and description
- **Due dates** as deadlines
- **Scheduled days** as intent
- **Optional time blocks** on the calendar grid
- **Completion tracking** with `completedAt` timestamps
- **Recurring tasks** with per-occurrence completion
- **Reorder** tasks within a day

### Conversion

- **Task → Event** when an item becomes time-bound
- **Event → Task** when an item stops being time-bound

### Views

- **My Day** — tasks scheduled for today
- **This Week** — everything scheduled or due this week
- **Backlog** — tasks with no schedule and no deadline
- **Completed** — finished tasks

### Derived

Served by the server through [Api.Summary](../api/controllers/Api.Summary.md) (and, for counts, the calendar list):

- **Streak** — consecutive days with at least one completed task
- **Focus Now** — the next upcoming event or scheduled task
- **Routine Start** — when to begin preparing for the next event
- **Overdue** — tasks past their deadline
- **Today's progress** — completed vs. total for the day
- **Calendar counts** — how many items per calendar

### Routine Start Reminder

A local notification on the client at an event's `routineStartAt` ("Time to start getting ready for *Meet friends*"). It is scheduled **on the device** from data the client already has, so it needs no server push, no `Reminder` model, and no scheduler on the server. It is what makes routines useful when the app is closed, and it is deliberately the *only* notification in v1; general reminders remain [deferred](#deferred).

### Cross-platform

- **Desktop** client for Windows, macOS, and Linux (Compose for Desktop)
- **Android** client for Android (Jetpack Compose)
- **Shared contract** — both clients use the same models, DTOs, validation, and API client from `planer-shared`
- **Forced update** — the server can require a minimum client version; older clients are asked to update

### Search

A global search across events and tasks. The API is only a sketch and the implementation is still undecided — see [Api.Search](../api/controllers/Api.Search.md) and [Architecture → Open Questions](../architecture/Architecture.md#open-questions).

## Deferred

Features that have been considered and intentionally postponed.
They may return in a future version.

- **Routine templates** — reusable preparation routines that can be applied to an event with one click. In v1 the routine lives directly on the event; templates would let the user save and reuse a common sequence (e.g., "Going out").
- **General reminders and notifications** — would require a `Reminder` model, a scheduler, and platform-specific notification code. (The [routine start reminder](#routine-start-reminder) is the one exception, done locally on the device.)
- **Attachments** — would require a storage layer for files and images
- **Daily check-in** — the "Energy Level Today" widget from early mockups
- **Offline mode** — a local cache so the app works without a network connection
- **iOS support** — a native SwiftUI client consuming `planer-shared`
- **Export / import** — iCal or CSV interchange with other calendar apps

## Explicitly Out of Scope

These are not planned, now or in the near future.

- **Sharing** with other users
- **Collaboration** on the same calendar
- **Multi-user workspaces or teams**
- **Public calendar publishing**
- **Third-party calendar sync** (Google Calendar, Outlook)

PlaneR is a single-user personal planner. Sharing and collaboration would fundamentally change the domain model (ownership becomes shared, permissions become non-trivial, conflicts need resolution) and are deliberately excluded to keep the app simple.