# PlaneR Features

This document lists what PlaneR does, in the current release.
It is split into **Implemented**, **Planned**, and **Deferred** so that readers
know what is real and what is an aspiration.

- [Implemented](#implemented)
- [Planned](#planned)
- [Deferred](#deferred)
- [Explicitly Out of Scope](#explicitly-out-of-scope)

## Implemented

> Nothing yet — the project is at the documentation stage.
> This section will be populated as features land.

## Planned

Features that are designed and will be built. The domain and API are already
shaped to support them.

### Accounts

- **Registration** with email and password
- **Login** with JWT access and refresh tokens
- **Email verification** before sensitive actions are allowed
- **Password reset** via email link
- **Change email** with re-verification of the new address
- **Change password** with sign-out of all other sessions
- **Account deletion** with cascading removal of all owned data

### Calendars

- **Multiple calendars** per user, each with a name, color, and icon
- **Default calendar** created on registration
- **Reorder** calendars in the sidebar
- **Delete** a calendar with options for its contents (unassign, reassign, cascade)

### Events

- **Create, edit, and delete** events with title, description, location, start, and end
- **All-day events** rendered as banners instead of time blocks
- **Recurring events** with daily, weekly, monthly, and yearly frequencies
- **Occurrence overrides** for editing a single instance of a recurring event
- **Skip occurrences** without breaking the series

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

- **Streak** — consecutive days with at least one completed task
- **Focus Now** — the next upcoming event or scheduled task
- **Overdue** — tasks past their deadline
- **Today's progress** — completed vs. total for the day
- **Calendar counts** — how many items per calendar

### Cross-platform

- **Desktop** client for Windows, macOS, and Linux
- **Mobile** client for Android
- **Shared UI** between both platforms via Compose Multiplatform

## Planned

Features that are designed but not yet built, beyond the ones already listed above.

### Search

A global search across events and tasks. Implementation is still undecided — see [Architecture → Open Questions](../architecture/Architecture.md#open-questions).

## Deferred

Features that have been considered and intentionally postponed.
They may return in a future version.

- **Reminders and notifications** — would require a `Reminder` model, a scheduler, and platform-specific notification code
- **Attachments** — would require a storage layer for files and images
- **Daily check-in** — the "Energy Level Today" widget from early mockups
- **Offline mode** — a local cache so the app works without a network connection
- **iOS support** — the Compose Multiplatform iOS target
- **Export / import** — iCal or CSV interchange with other calendar apps

## Explicitly Out of Scope

These are not planned, now or in the near future.

- **Sharing** with other users
- **Collaboration** on the same calendar
- **Multi-user workspaces or teams**
- **Public calendar publishing**
- **Third-party calendar sync** (Google Calendar, Outlook)

PlaneR is a single-user personal planner. Sharing and collaboration would fundamentally change the domain model (ownership becomes shared, permissions become non-trivial, conflicts need resolution) and are deliberately excluded to keep the app simple.