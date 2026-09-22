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

A to-do item. May or may not be scheduled.

---

## Get Collection

```js
GET {{host}}/api/tasks?from=2025-07-13&to=2025-07-19&calendarIds=11111111-...&completed=false&unassigned=false&page=0&size=20
```

**Query parameters**

| Param | Required | Notes |
|---|---|---|
| `from` | no | Start of range, inclusive |
| `to` | no | End of range, inclusive |
| `calendarIds` | no | Comma-separated list |
| `completed` | no | `true` / `false`; omit for both |
| `unassigned` | no | If `true`, only tasks with `calendarId = null` |
| `backlog` | no | If `true`, only tasks with `scheduledFor == null && dueAt == null` |
| `page` | no | Default `0` |
| `size` | no | Default `20`, max `100` |

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
      "completed": false,
      "completedAt": null,
      "position": 0,
      "recurrence": null
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
      "completed": false,
      "completedAt": null,
      "position": 1,
      "recurrence": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 2,
  "totalPages": 1
}
```

---

## Get

```js
GET {{host}}/api/tasks/{id}
```

**Response**

A single task, same shape as an entry in **Get Collection**.

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
  "recurrence": null
}
```

| Field | Required | Notes |
|---|---|---|
| `calendarId` | no | `null` for unassigned |
| `title` | yes | 1–120 chars |
| `description` | no | Max 2000 chars |
| `dueAt` | no | ISO-8601 UTC |
| `scheduledFor` | no | `yyyy-MM-dd` |
| `scheduledStartAt` | no | ISO-8601 UTC; requires `scheduledFor` |
| `scheduledEndAt` | no | ISO-8601 UTC; requires `scheduledStartAt` |
| `recurrence` | no | `RecurrenceRule` object or `null` |

**Response**

The created task, same shape as **Get**.

---

## Update

```js
PUT {{host}}/api/tasks/{id}
```

**Query parameters**

| Param | Values | Default | Meaning |
|---|---|---|---|
| `scope` | `single` \| `future` \| `all` | `all` | Only relevant for recurring tasks |

**Request**

Any subset of the fields from **Create**.

**Response**

```json
{
  "message": "Task updated."
}
```

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

**Response**

```json
{
  "message": "Task completed.",
  "completedAt": "2025-07-15T13:45:00Z"
}
```

---

## Uncomplete

Reverts a task to the pending state.

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

```json
{
  "message": "Task marked as pending."
}
```

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
in the same call.

**Response**

```json
{
  "message": "Task scheduled."
}
```

---

## Unschedule

Removes the task from the day view, keeping its other fields intact.

```js
PUT {{host}}/api/tasks/{id}/unschedule
```

**Response**

```json
{
  "message": "Task unscheduled."
}
```

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

| Param | Values | Default | Meaning |
|---|---|---|---|
| `scope` | `single` \| `future` \| `all` | `all` | Only relevant for recurring tasks |

**Response**

```json
{
  "message": "Task deleted."
}
```

---

## Convert to Event

Deletes the task and creates an event from its fields.
The task's `scheduledStartAt` / `scheduledEndAt` become the event's `startsAt` / `endsAt`,
unless overridden in the request.

```js
POST {{host}}/api/tasks/{id}/convert-to-event
```

**Request**

```json
{
  "startsAt": "2025-07-15T09:00:00Z",
  "endsAt": "2025-07-15T09:30:00Z",
  "allDay": false
}
```

**Response**

The newly created event, same shape as **Event → Get**.

---

## Override Occurrence

Same as **Event → Override Occurrence**, for recurring tasks.

```js
PUT {{host}}/api/tasks/{id}/occurrences/{date}
```

---

## Skip Occurrence

Same as **Event → Skip Occurrence**, for recurring tasks.

```js
DELETE {{host}}/api/tasks/{id}/occurrences/{date}
```

---

## Bulk Delete

Deletes multiple tasks in a single request.
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

| Field | Required | Notes |
|---|---|---|
| `ids` | yes | 1–100 items, all owned by the user |

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

---

## Bulk Complete

Marks multiple tasks as completed. Non-recurring tasks only, or a single
occurrence per recurring task.

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

`occurrenceDate` is required when the task is recurring.

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

**Request and response** — same shape as **Bulk Complete**.

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

- `404 Not Found` if `calendarId` is not owned by the caller

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
Positions are reassigned as `0, 1, 2, …`.

**Response**

```json
{
  "message": "Order updated."
}
```