# PlaneR Api — Event

- [Get Collection](#get-collection)
- [Get](#get)
- [Create](#create)
- [Update](#update)
- [Move](#move)
- [Delete](#delete)
- [Override Occurrence](#override-occurrence)
- [Skip Occurrence](#skip-occurrence)
- [Convert to Task](#convert-to-task)
- [Bulk Delete](#bulk-delete)
- [Bulk Shift](#bulk-shift)
- [Bulk Move](#bulk-move)

A time-bound occurrence. Always has a `startsAt` and `endsAt`.
May carry a **routine** — an ordered list of preparation steps the user performs
before the event starts, used for reverse planning. See
[Domain → Preparation Steps](../../domain/Domain.md#preparation-steps).

---

## Get Collection

Returns events in a date range. Recurring events are expanded into their occurrences
within the range.

```js
GET {{host}}/api/events?from=2025-07-13&to=2025-07-19&calendarIds=11111111-...&unassigned=false&page=0&size=20
```

**Query parameters**

| Param | Required | Notes |
|---|---|---|
| `from` | yes | Start of range, inclusive (ISO date or timestamp) |
| `to` | yes | End of range, inclusive |
| `calendarIds` | no | Comma-separated list; omit to include all calendars |
| `unassigned` | no | If `true`, only events with `calendarId = null` |
| `page` | no | Default `0` |
| `size` | no | Default `20`, max `100` |

**Response**

```json
{
  "items": [
    {
      "id": "22222222-0000-7000-8000-000000000001",
      "calendarId": "11111111-0000-7000-8000-000000000001",
      "title": "Meet friends",
      "description": null,
      "location": "Café Central",
      "startsAt": "2025-07-15T17:30:00Z",
      "endsAt": "2025-07-15T20:00:00Z",
      "allDay": false,
      "recurrence": null,
      "routineStartAt": "2025-07-15T15:30:00Z",
      "routine": [
        { "name": "Shower",   "duration": 40, "startsAt": "2025-07-15T15:30:00Z", "endsAt": "2025-07-15T16:10:00Z" },
        { "name": "Dress",    "duration": 30, "startsAt": "2025-07-15T16:10:00Z", "endsAt": "2025-07-15T16:40:00Z" },
        { "name": "Walk dog", "duration": 20, "startsAt": "2025-07-15T16:40:00Z", "endsAt": "2025-07-15T17:00:00Z" },
        { "name": "Travel",   "duration": 30, "startsAt": "2025-07-15T17:00:00Z", "endsAt": "2025-07-15T17:30:00Z" }
      ],
      "occurrenceDate": "2025-07-15",
      "isRecurringInstance": false
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

`occurrenceDate` is present only for expanded occurrences of a recurring event.

`routineStartAt` is `null` when the event has no routine.

---

## Get

```js
GET {{host}}/api/events/{id}
```

**Response**

The event object, same shape as a single entry in **Get Collection**,
without `occurrenceDate` / `isRecurringInstance`.

---

## Create

```js
POST {{host}}/api/events
```

**Request**

```json
{
  "calendarId": "11111111-0000-7000-8000-000000000001",
  "title": "Meet friends",
  "description": null,
  "location": "Café Central",
  "startsAt": "2025-07-15T17:30:00Z",
  "endsAt": "2025-07-15T20:00:00Z",
  "allDay": false,
  "recurrence": null,
  "routine": [
    { "name": "Shower",   "duration": 40 },
    { "name": "Dress",    "duration": 30 },
    { "name": "Walk dog", "duration": 20 },
    { "name": "Travel",   "duration": 30 }
  ]
}
```

| Field | Required | Notes |
|---|---|---|
| `calendarId` | no | `null` for unassigned |
| `title` | yes | 1–120 chars |
| `description` | no | Max 2000 chars |
| `location` | no | Max 200 chars |
| `startsAt` | yes | ISO-8601 UTC |
| `endsAt` | yes | ISO-8601 UTC, must be after `startsAt` |
| `allDay` | no | Default `false` |
| `recurrence` | no | `RecurrenceRule` object or `null` |
| `routine` | no | Ordered list; omit or pass `[]` for none. Max 20 steps. |

**Routine validation**

- `name` — 1–60 characters, trimmed
- `duration` — 1 to 1440 (in minutes; 24 hours max per step)
- Total routine duration must not exceed **24 hours** (1440 minutes). An event whose steps add up to more than a day is almost certainly a data-entry mistake.

**Response**

The created event, same shape as **Get**. `routineStartAt` and per-step
`startsAt`/`endsAt` are computed by the server.

**Errors**

- `400 Bad Request` if `endsAt` is not after `startsAt`, if a routine step fails validation, or if the total routine duration exceeds 24 hours
- `404 Not Found` if `calendarId` is not owned by the user

---

## Update

```js
PUT {{host}}/api/events/{id}
```

**Query parameters**

| Param | Values | Default | Meaning |
|---|---|---|---|
| `scope` | `single` \| `future` \| `all` | `all` | Only relevant for recurring events |

**Request**

Any subset of the fields from **Create**.

If `routine` is provided, it **replaces** the entire list. There is no partial
update of individual steps — the client sends the full list every time.

**Response**

```json
{
  "message": "Event updated."
}
```

**Errors**

- `400 Bad Request` if the request violates the time or routine constraints
- `404 Not Found` if the event is not owned by the user

---

## Move

Convenience endpoint for drag-and-drop. Equivalent to calling **Update** with new times.
Routine steps are laid out backward from the new `startsAt` automatically.

```js
PUT {{host}}/api/events/{id}/move
```

**Request**

```json
{
  "startsAt": "2025-07-15T18:00:00Z",
  "endsAt": "2025-07-15T20:30:00Z"
}
```

**Response**

```json
{
  "message": "Event moved.",
  "routineStartAt": "2025-07-15T16:00:00Z"
}
```

The response includes the recomputed `routineStartAt` so the client can update
the grid without a refetch.

---

## Delete

```js
DELETE {{host}}/api/events/{id}
```

**Query parameters**

| Param | Values | Default | Meaning |
|---|---|---|---|
| `scope` | `single` \| `future` \| `all` | `all` | Only relevant for recurring events |

**Response**

```json
{
  "message": "Event deleted."
}
```

---

## Override Occurrence

Creates or updates a `RecurrenceException` for a single occurrence of a recurring event.

```js
PUT {{host}}/api/events/{id}/occurrences/{date}
```

- `date` — occurrence date in `yyyy-MM-dd`

**Request**

Same shape as **Update**, but applies only to the given occurrence.

**Response**

```json
{
  "message": "Occurrence updated."
}
```

---

## Skip Occurrence

Marks a single occurrence as skipped.

```js
DELETE {{host}}/api/events/{id}/occurrences/{date}
```

**Response**

```json
{
  "message": "Occurrence skipped."
}
```

---

## Convert to Task

Deletes the event and creates a task from its fields.
The event's `routine` is **discarded** — a Task has no preparation concept.

```js
POST {{host}}/api/events/{id}/convert-to-task
```

**Request**

```json
{
  "dueAt": "2025-07-20T17:00:00Z",
  "scheduledFor": "2025-07-19"
}
```

All fields are optional; if omitted, the event's times are used as a starting point.

**Response**

The newly created task, same shape as **Task → Get**.

---

## Bulk Delete

Deletes multiple events in a single request.

```js
POST {{host}}/api/events/bulk/delete
```

**Request**

```json
{
  "ids": [
    "22222222-0000-7000-8000-000000000001",
    "22222222-0000-7000-8000-000000000002"
  ],
  "scope": "all"
}
```

| Field | Required | Notes |
|---|---|---|
| `ids` | yes | 1–100 items, all owned by the user |
| `scope` | no | `single` \| `future` \| `all`, default `all`; only relevant for recurring events |

**Response**

```json
{
  "deleted": [
    "22222222-0000-7000-8000-000000000001",
    "22222222-0000-7000-8000-000000000002"
  ],
  "notFound": []
}
```

---

## Bulk Shift

Moves multiple events by the same time delta. Useful for "push everything back by an hour".

```js
POST {{host}}/api/events/bulk/shift
```

**Request**

```json
{
  "ids": [
    "22222222-0000-7000-8000-000000000001",
    "22222222-0000-7000-8000-000000000002"
  ],
  "deltaMinutes": 60
}
```

Shifts `startsAt` and `endsAt` of every event by the same amount.
The routine shifts along with the event — the total preparation duration is
unchanged, so `routineStartAt` shifts by the same delta.

**Response**

```json
{
  "shifted": [
    "22222222-0000-7000-8000-000000000001",
    "22222222-0000-7000-8000-000000000002"
  ],
  "notFound": []
}
```

---

## Bulk Move

Moves multiple events to another calendar (or to unassigned).

```js
POST {{host}}/api/events/bulk/move
```

**Request**

```json
{
  "calendarId": "11111111-0000-7000-8000-000000000003",
  "ids": [
    "22222222-0000-7000-8000-000000000001",
    "22222222-0000-7000-8000-000000000002"
  ]
}
```

`calendarId` may be `null` to unassign.

**Response**

```json
{
  "moved": [
    "22222222-0000-7000-8000-000000000001",
    "22222222-0000-7000-8000-000000000002"
  ],
  "notFound": []
}
```

**Errors**

- `404 Not Found` if `calendarId` is not owned by the caller