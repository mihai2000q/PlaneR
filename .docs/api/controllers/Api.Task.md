# PlaneR Api — Task

- [Get Collection](#get-collection)
- [Get](#get)
- [Create](#create)
- [Update](#update)
- [Complete](#complete)
- [Uncomplete](#uncomplete)
- [Schedule](#schedule)
- [Unschedule](#unschedule)
- [Reorder](#reorder)
- [Delete](#delete)
- [Convert to Event](#convert-to-event)
- [Override Occurrence](#override-occurrence)
- [Skip Occurrence](#skip-occurrence)
- [Bulk Delete](#bulk-delete)
- [Bulk Complete](#bulk-complete)
- [Bulk Uncomplete](#bulk-uncomplete)
- [Bulk Schedule](#bulk-schedule)
- [Bulk Unschedule](#bulk-unschedule)
- [Bulk Move](#bulk-move)
- [Bulk Reorder](#bulk-reorder)

A to-do item. May or may not be scheduled.

Like events, tasks carry a `timeZone` (an IANA identifier, defaulting to the user's
current timezone). It defines what "the local date" of a time block is and how a
recurring task is expanded across daylight-saving changes. See
[Domain → Timezones](../../domain/Domain.md#timezones).

Recurring tasks are expanded into occurrences on read, exactly as recurring events are
(see [Api.Event](Api.Event.md#recurring-events-scopes-and-occurrences)). **Completion is per occurrence.**

---

## Get Collection

```js
GET {{host}}/api/tasks?from=2025-07-13&to=2025-07-19&calendarIds=11111111-...&completed=false&unassigned=false
```

**Query parameters**

| Param         | Required | Notes                                                                                                        |
|---------------|----------|--------------------------------------------------------------------------------------------------------------|
| `from`        | no       | First day of a date range, `yyyy-MM-dd`, inclusive, in the user's timezone. Must be sent together with `to`. |
| `to`          | no       | Last day of the range, inclusive. Must be sent together with `from`.                                         |
| `calendarIds` | no       | Comma-separated list                                                                                         |
| `completed`   | no       | `true` / `false`; omit for both                                                                              |
| `unassigned`  | no       | If `true`, only tasks with `calendarId = null`                                                               |
| `backlog`     | no       | If `true`, only tasks with `scheduledFor == null && dueAt == null`                                           |
| `overdue`     | no       | If `true`, only tasks with `dueAt` in the past that are not completed                                        |
| `page`        | no       | Default `0`. Ignored when `from`/`to` are sent.                                                              |
| `size`        | no       | Default `20`, max `100`. Ignored when `from`/`to` are sent.                                                  |

**Date-range queries.** A task matches `from`/`to` when its `scheduledFor` **or** the
local date of its `dueAt` falls inside the range. This is what the "This Week" view
needs. Recurring tasks are expanded: each occurrence whose `scheduledFor` (or due date)
falls in the range is returned. A range query is **not paginated**, may not exceed **366 days**, and returns
`{ "items": [ ... ] }` (see [Api → Pagination](../Api.md#pagination)).

**Without a range**, the result is paginated with the standard envelope. This serves
the Backlog, Completed, and Overdue views. Recurring tasks are **not** expanded here;
they appear once, as the series, with `occurrenceDate: null`. To see occurrences, query
by range.

`backlog`, `overdue`, and `completed` combine with the range and with each other using AND.

**Sorting.** `scheduledFor` ascending (unscheduled last), then `position`, then `createdAt`.

**Response**

```json
{
  "items": [
    {
      "id": "33333333-0000-7000-8000-000000000001",
      "calendarId": "11111111-0000-7000-8000-000000000002",
      "title": "Buy groceries",
      "description": null,
      "dueAt": null,
      "scheduledFor": "2025-07-15",
      "scheduledStartAt": null,
      "scheduledEndAt": null,
      "timeZone": "Europe/Berlin",
      "completed": false,
      "completedAt": null,
      "position": 0,
      "recurrence": null,
      "createdAt": "2025-07-10T08:00:00Z",
      "updatedAt": "2025-07-10T08:00:00Z",
      "occurrenceDate": null,
      "isRecurringInstance": false
    },
    {
      "id": "33333333-0000-7000-8000-000000000002",
      "calendarId": "11111111-0000-7000-8000-000000000002",
      "title": "Car wash",
      "description": null,
      "dueAt": "2025-07-18T17:00:00Z",
      "scheduledFor": "2025-07-18",
      "scheduledStartAt": "2025-07-18T13:00:00Z",
      "scheduledEndAt": "2025-07-18T15:00:00Z",
      "timeZone": "Europe/Berlin",
      "completed": false,
      "completedAt": null,
      "position": 1,
      "recurrence": null,
      "createdAt": "2025-07-10T08:05:00Z",
      "updatedAt": "2025-07-10T08:05:00Z",
      "occurrenceDate": null,
      "isRecurringInstance": false
    }
  ]
}
```

For an expanded occurrence of a recurring task, `occurrenceDate` is the occurrence's
local date, `isRecurringInstance` is `true`, `id` is the series' id, and `completed` /
`completedAt` describe **that occurrence**.

`position` orders tasks **within one scheduled day**. It is meaningful only when
`scheduledFor` is set; unscheduled tasks have `position = 0`.

---

## Get

```js
GET {{host}}/api/tasks/{id}
```

**Response**

A single task, same shape as an entry in **Get Collection**. For a recurring task it is
the series, with `occurrenceDate: null`, and `completed` reflects the series' own flag (always `false` for a recurring
task, whose completions are per occurrence).

---

## Create

```js
POST {{host}}/api/tasks
```

**Request**

```json
{
  "calendarId": "11111111-0000-7000-8000-000000000002",
  "title": "Call dentist",
  "description": null,
  "dueAt": "2025-07-15T15:30:00Z",
  "scheduledFor": "2025-07-15",
  "scheduledStartAt": null,
  "scheduledEndAt": null,
  "timeZone": "Europe/Berlin",
  "recurrence": null
}
```

| Field              | Required | Notes                                                                                       |
|--------------------|----------|---------------------------------------------------------------------------------------------|
| `calendarId`       | no       | `null` for unassigned                                                                       |
| `title`            | yes      | 1–120 chars                                                                                 |
| `description`      | no       | Max 2000 chars                                                                              |
| `dueAt`            | no       | ISO-8601 UTC                                                                                |
| `scheduledFor`     | no       | `yyyy-MM-dd`                                                                                |
| `scheduledStartAt` | no       | ISO-8601 UTC; requires `scheduledFor`, and must fall on that date in `timeZone`             |
| `scheduledEndAt`   | no       | ISO-8601 UTC; requires `scheduledStartAt`, and must be after it                             |
| `timeZone`         | no       | IANA identifier; defaults to the user's current timezone                                    |
| `recurrence`       | no       | `RecurrenceRule` object or `null`. Requires `scheduledFor` or `dueAt` to anchor the series. |

**Response**

The created task, same shape as **Get**.

**Errors**

- `400 VALIDATION_ERROR` if a field is invalid, a time block does not match `scheduledFor`, or `scheduledEndAt` is not
  after `scheduledStartAt`
- `404 NOT_FOUND` if `calendarId` is not owned by the user

---

## Update

Partially updates a task, using JSON merge-patch semantics (see [Api → Updates](../Api.md#updates)).

```js
PATCH {{host}}/api/tasks/{id}
```

**Query parameters**

| Param            | Values            | Default | Meaning                                                                  |
|------------------|-------------------|---------|--------------------------------------------------------------------------|
| `scope`          | `all` \| `future` | `all`   | Only relevant for recurring tasks                                        |
| `occurrenceDate` | `yyyy-MM-dd`      | —       | Required when `scope=future`: the first occurrence the change applies to |

`future` splits the series at `occurrenceDate`, as for events. To change one occurrence,
use [Override Occurrence](#override-occurrence).

**Request**

Any subset of the fields from **Create**.

**Response**

The updated task, same shape as **Get**. With `scope=future` it is the **new** series.

---

## Complete

Marks a task as completed.
For recurring tasks, this applies to a single occurrence, identified by `occurrenceDate`.

```js
PUT {{host}}/api/tasks/{id}/complete
```

**Request**

```json
{
  "occurrenceDate": "2025-07-15"
}
```

`occurrenceDate` is required for recurring tasks, ignored for non-recurring ones.

The operation is idempotent: completing an already-completed task changes nothing and
keeps the **original** `completedAt`.

**Response**

The task (or the occurrence), same shape as in **Get Collection**, with `completed: true`
and `completedAt` set.

**Errors**

- `400 VALIDATION_ERROR` if the task is recurring and `occurrenceDate` is missing or is not an occurrence of it

---

## Uncomplete

Reverts a task to the pending state, clearing `completedAt`.
For recurring tasks, `occurrenceDate` is required, as for **Complete**.

```js
PUT {{host}}/api/tasks/{id}/uncomplete
```

**Request**

```json
{
  "occurrenceDate": "2025-07-15"
}
```

**Response**

The task (or the occurrence), same shape as in **Get Collection**, with `completed: false`.

---

## Schedule

Assigns a task to a day, with optional time block.

```js
PUT {{host}}/api/tasks/{id}/schedule
```

**Request**

```json
{
  "scheduledFor": "2025-07-16",
  "scheduledStartAt": "2025-07-16T14:00:00Z",
  "scheduledEndAt": "2025-07-16T15:00:00Z"
}
```

`calendarId` may also be included to reassign the task to a different calendar
in the same call. The time-block rules from **Create** apply.

**Response**

The updated task, same shape as **Get**.

---

## Unschedule

Removes the task from the day view, keeping its other fields intact.
`scheduledFor`, `scheduledStartAt`, and `scheduledEndAt` become `null`, and `position` resets to `0`.

```js
PUT {{host}}/api/tasks/{id}/unschedule
```

**Response**

The updated task, same shape as **Get**.

---

## Reorder

Updates the manual ordering of a task within a day.

```js
PUT {{host}}/api/tasks/{id}/position
```

**Request**

```json
{
  "scheduledFor": "2025-07-15",
  "newPosition": 2
}
```

`scheduledFor` must be the task's current scheduled day. To move a task to another day **and** position,
use [Schedule](#schedule) and then Reorder.

**Response**

```json
{
  "message": "Position updated."
}
```

---

## Delete

```js
DELETE {{host}}/api/tasks/{id}
```

**Query parameters**

| Param            | Values            | Default | Meaning                                                                |
|------------------|-------------------|---------|------------------------------------------------------------------------|
| `scope`          | `all` \| `future` | `all`   | Only relevant for recurring tasks                                      |
| `occurrenceDate` | `yyyy-MM-dd`      | —       | Required when `scope=future`: the series ends the day before this date |

**Response**

```json
{
  "message": "Task deleted."
}
```

---

## Convert to Event

Converts the task into an event. The operation is **atomic**: the task is deleted and
the event created in one transaction, or neither happens.

```js
POST {{host}}/api/tasks/{id}/convert-to-event
```

An event must have a start and an end. They come from, in order of priority:

1. `startsAt` / `endsAt` in the request
2. the task's `scheduledStartAt` / `scheduledEndAt`
3. if the request sets `allDay: true`, the local day of `scheduledFor` (or of the request's `scheduledFor`)

If none of these yields a time, the request fails with `400 VALIDATION_ERROR`. A task with
only a `dueAt` or a bare `scheduledFor` therefore needs the request to say when the event is.

What carries over:

| Task                                                           | Event                                           |
|----------------------------------------------------------------|-------------------------------------------------|
| `title`, `description`, `calendarId`, `timeZone`, `recurrence` | same                                            |
| `createdAt`                                                    | same (the event keeps the task's creation time) |
| `dueAt`, `position`, `completed`, `completedAt`                | dropped                                         |
| occurrence overrides, skips, and completions                   | dropped; only the series itself converts        |

The new event starts with an empty `routine` and gets a **new id**; the task's id stops existing.

**Request**

```json
{
  "startsAt": "2025-07-15T09:00:00Z",
  "endsAt": "2025-07-15T09:30:00Z",
  "allDay": false
}
```

All fields are optional.

**Response**

The newly created event, same shape as **Event → Get**.

---

## Override Occurrence

Creates or replaces the `RecurrenceException` for a single occurrence of a recurring task,
as for [Event → Override Occurrence](Api.Event.md#override-occurrence).

```js
PUT {{host}}/api/tasks/{id}/occurrences/{date}
```

**Request**

The overridable fields are `title`, `description`, `dueAt`, `scheduledFor`,
`scheduledStartAt`, and `scheduledEndAt`. Fields not sent keep the series' values.
Completion is not an override — use [Complete](#complete).

**Response**

The occurrence as it now appears in **Get Collection**.

---

## Skip Occurrence

Marks a single occurrence of a recurring task as skipped, as for
[Event → Skip Occurrence](Api.Event.md#skip-occurrence).

```js
DELETE {{host}}/api/tasks/{id}/occurrences/{date}
```

---

## Bulk Delete

Deletes multiple tasks (whole tasks, including all occurrences of recurring ones) in a single request.
Returns per-id results so the client can reconcile partial failures.

```js
POST {{host}}/api/tasks/bulk/delete
```

**Request**

```json
{
  "ids": [
    "33333333-0000-7000-8000-000000000001",
    "33333333-0000-7000-8000-000000000002",
    "33333333-0000-7000-8000-000000000003"
  ]
}
```

| Field | Required | Notes       |
|-------|----------|-------------|
| `ids` | yes      | 1–100 items |

**Response**

```json
{
  "deleted": [
    "33333333-0000-7000-8000-000000000001",
    "33333333-0000-7000-8000-000000000003"
  ],
  "notFound": [
    "33333333-0000-7000-8000-000000000002"
  ]
}
```

`notFound` covers ids that do not exist or are not owned by the caller.
The endpoint never fails the whole batch because of one missing id.
The same rule applies to every bulk endpoint below.

---

## Bulk Complete

Marks multiple tasks as completed. Non-recurring tasks, or a single occurrence per recurring task.

```js
POST {{host}}/api/tasks/bulk/complete
```

**Request**

```json
{
  "items": [
    { "id": "33333333-0000-7000-8000-000000000001" },
    { "id": "33333333-0000-7000-8000-000000000002", "occurrenceDate": "2025-07-15" }
  ]
}
```

`occurrenceDate` is required when the task is recurring. An item that is already
completed is reported as completed and left unchanged.

**Response**

```json
{
  "completed": [
    "33333333-0000-7000-8000-000000000001",
    "33333333-0000-7000-8000-000000000002"
  ],
  "notFound": []
}
```

---

## Bulk Uncomplete

Reverts multiple tasks to pending.

```js
POST {{host}}/api/tasks/bulk/uncomplete
```

**Request** — same shape as **Bulk Complete**.

**Response**

```json
{
  "uncompleted": [
    "33333333-0000-7000-8000-000000000001",
    "33333333-0000-7000-8000-000000000002"
  ],
  "notFound": []
}
```

---

## Bulk Schedule

Assigns multiple tasks to a day at once. Useful for the "drag selection into My Day" flow.

```js
POST {{host}}/api/tasks/bulk/schedule
```

**Request**

```json
{
  "scheduledFor": "2025-07-16",
  "ids": [
    "33333333-0000-7000-8000-000000000001",
    "33333333-0000-7000-8000-000000000002"
  ]
}
```

`scheduledStartAt` and `scheduledEndAt` may be included to give all tasks the same
time block, but this is uncommon — most bulk scheduling is date-only.

**Response**

```json
{
  "scheduled": [
    "33333333-0000-7000-8000-000000000001",
    "33333333-0000-7000-8000-000000000002"
  ],
  "notFound": []
}
```

---

## Bulk Unschedule

Removes multiple tasks from their scheduled day.

```js
POST {{host}}/api/tasks/bulk/unschedule
```

**Request**

```json
{
  "ids": [
    "33333333-0000-7000-8000-000000000001",
    "33333333-0000-7000-8000-000000000002"
  ]
}
```

**Response**

```json
{
  "unscheduled": [
    "33333333-0000-7000-8000-000000000001",
    "33333333-0000-7000-8000-000000000002"
  ],
  "notFound": []
}
```

---

## Bulk Move

Moves multiple tasks to another calendar (or to unassigned).

```js
POST {{host}}/api/tasks/bulk/move
```

**Request**

```json
{
  "calendarId": "11111111-0000-7000-8000-000000000003",
  "ids": [
    "33333333-0000-7000-8000-000000000001",
    "33333333-0000-7000-8000-000000000002"
  ]
}
```

`calendarId` may be `null` to unassign.

**Response**

```json
{
  "moved": [
    "33333333-0000-7000-8000-000000000001",
    "33333333-0000-7000-8000-000000000002"
  ],
  "notFound": []
}
```

**Errors**

- `404 NOT_FOUND` if `calendarId` is not owned by the caller

---

## Bulk Reorder

Updates the `position` of several tasks within the same day.

```js
POST {{host}}/api/tasks/bulk/reorder
```

**Request**

```json
{
  "scheduledFor": "2025-07-15",
  "order": [
    "33333333-0000-7000-8000-000000000003",
    "33333333-0000-7000-8000-000000000001",
    "33333333-0000-7000-8000-000000000002"
  ]
}
```

The `order` array is the definitive new order for that day.
Positions are reassigned as `0, 1, 2, …`. Every id must be a task scheduled for
`scheduledFor`; otherwise `400 VALIDATION_ERROR`.

**Response**

```json
{
  "message": "Order updated."
}
```
