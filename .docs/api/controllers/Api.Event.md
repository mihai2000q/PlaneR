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

Every event has a `timeZone` (an IANA identifier). Recurrence, all-day boundaries, and
daylight-saving transitions are computed in that zone, so a 09:00 standup stays at
09:00 local time all year. See [Domain → Timezones](../../domain/Domain.md#timezones).

### Recurring events: scopes and occurrences

A recurring event is stored once and expanded into occurrences on read. There are two
ways to change it:

- **The whole series, or a series from some point on** — [Update](#update) and
  [Delete](#delete) take `scope=all` or `scope=future`. `future` splits the series at
  `occurrenceDate`, so it is required with `future`.
- **One occurrence** — [Override Occurrence](#override-occurrence) and
  [Skip Occurrence](#skip-occurrence), addressed by date.

---

## Get Collection

Returns events overlapping a date range. Recurring events are expanded into their
occurrences within the range.

```js
GET {{host}}/api/events?from=2025-07-13&to=2025-07-19&calendarIds=11111111-...&unassigned=false
```

**Query parameters**

| Param         | Required | Notes                                                                   |
|---------------|----------|-------------------------------------------------------------------------|
| `from`        | yes      | First day of the range, `yyyy-MM-dd`, inclusive, in the user's timezone |
| `to`          | yes      | Last day of the range, `yyyy-MM-dd`, inclusive, in the user's timezone  |
| `calendarIds` | no       | Comma-separated list; omit to include all calendars                     |
| `unassigned`  | no       | If `true`, only events with `calendarId = null`                         |

- The range covers whole days: from the start of `from` to the end of `to`, in the user's timezone.
- An event is returned if it **overlaps** the range at all, so an event that starts the day before `from` and ends
  inside the range is included.
- The range must not exceed **366 days**; otherwise `400 VALIDATION_ERROR`.
- The result is **not paginated** (see [Api → Pagination](../Api.md#pagination)) and is sorted by `startsAt`, then `id`.

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
      "timeZone": "Europe/Berlin",
      "allDay": false,
      "recurrence": null,
      "routineStartAt": "2025-07-15T15:30:00Z",
      "routine": [
        { "name": "Shower",   "duration": 40, "startsAt": "2025-07-15T15:30:00Z", "endsAt": "2025-07-15T16:10:00Z" },
        { "name": "Dress",    "duration": 30, "startsAt": "2025-07-15T16:10:00Z", "endsAt": "2025-07-15T16:40:00Z" },
        { "name": "Walk dog", "duration": 20, "startsAt": "2025-07-15T16:40:00Z", "endsAt": "2025-07-15T17:00:00Z" },
        { "name": "Travel",   "duration": 30, "startsAt": "2025-07-15T17:00:00Z", "endsAt": "2025-07-15T17:30:00Z" }
      ],
      "createdAt": "2025-07-01T10:00:00Z",
      "updatedAt": "2025-07-01T10:00:00Z",
      "occurrenceDate": null,
      "isRecurringInstance": false
    }
  ]
}
```

`occurrenceDate` is the occurrence's local date (`yyyy-MM-dd`) for expanded occurrences of a recurring event, and `null`
otherwise. For an occurrence, `startsAt`/`endsAt`/`routine` are those of the occurrence, while `id` is the series' id.

`routineStartAt` is `null` when the event has no routine.

---

## Get

```js
GET {{host}}/api/events/{id}
```

**Response**

The event object, same shape as a single entry in **Get Collection**. For a
recurring event it is the series itself (the first occurrence's times), with
`occurrenceDate` and `isRecurringInstance` both `null`/`false`.

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
  "timeZone": "Europe/Berlin",
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

| Field         | Required | Notes                                                                                        |
|---------------|----------|----------------------------------------------------------------------------------------------|
| `calendarId`  | no       | `null` for unassigned                                                                        |
| `title`       | yes      | 1–120 chars                                                                                  |
| `description` | no       | Max 2000 chars                                                                               |
| `location`    | no       | Max 200 chars                                                                                |
| `startsAt`    | yes      | ISO-8601 UTC                                                                                 |
| `endsAt`      | yes      | ISO-8601 UTC, must be after `startsAt`                                                       |
| `timeZone`    | no       | IANA identifier; defaults to the user's current timezone                                     |
| `allDay`      | no       | Default `false`. When `true`, `startsAt` and `endsAt` must be local midnights in `timeZone`. |
| `recurrence`  | no       | `RecurrenceRule` object or `null`                                                            |
| `routine`     | no       | Ordered list; omit or pass `[]` for none. Max 20 steps. Not allowed on all-day events.       |

**Routine validation**

- `name` — 1–60 characters, trimmed
- `duration` — 1 to 1440 (in minutes; 24 hours max per step)
- Total routine duration must not exceed **24 hours** (1440 minutes). An event whose steps add up to more than a day is
  almost certainly a data-entry mistake.
- An event with `allDay = true` cannot have a routine: preparing "before" an all-day banner has no meaning.

**Response**

```json
{
  "message": "Event created."
}
```

`routineStartAt` and the per-step `startsAt`/`endsAt` are computed by the server; read them with **Get**.

**Errors**

- `400 VALIDATION_ERROR` if `endsAt` is not after `startsAt`, if `timeZone` is not a valid identifier, if an all-day
  event's times are not local midnights, if a routine step fails validation, if the total routine duration exceeds 24
  hours, or if an all-day event has a routine
- `404 NOT_FOUND` if `calendarId` is not owned by the user

---

## Update

Partially updates an event, using JSON merge-patch semantics (see [Api → Updates](../Api.md#updates)).

```js
PATCH {{host}}/api/events/{id}
```

**Query parameters**

| Param            | Values            | Default | Meaning                                                                  |
|------------------|-------------------|---------|--------------------------------------------------------------------------|
| `scope`          | `all` \| `future` | `all`   | Only relevant for recurring events                                       |
| `occurrenceDate` | `yyyy-MM-dd`      | —       | Required when `scope=future`: the first occurrence the change applies to |

With `scope=future` the series is split at `occurrenceDate`: the original series is
ended the day before, and a new series carrying the changes starts on that date.
To change a single occurrence, use [Override Occurrence](#override-occurrence).

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

With `scope=future` the changes live in the **new** series; read the events again to get its id.

**Errors**

- `400 VALIDATION_ERROR` if the request violates the time or routine constraints, or if `scope=future` is used without
  `occurrenceDate`
- `404 NOT_FOUND` if the event is not owned by the user

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
  "endsAt": "2025-07-15T20:30:00Z",
  "occurrenceDate": null
}
```

For a recurring event, pass `occurrenceDate` to move **just that occurrence** (it
becomes an [override](#override-occurrence)). Without it, the whole series is moved:
`startsAt`/`endsAt` are the new times of the series' first occurrence.

**Response**

```json
{
  "message": "Event moved."
}
```

---

## Delete

```js
DELETE {{host}}/api/events/{id}
```

**Query parameters**

| Param            | Values            | Default | Meaning                                                                |
|------------------|-------------------|---------|------------------------------------------------------------------------|
| `scope`          | `all` \| `future` | `all`   | Only relevant for recurring events                                     |
| `occurrenceDate` | `yyyy-MM-dd`      | —       | Required when `scope=future`: the series ends the day before this date |

To remove a single occurrence, use [Skip Occurrence](#skip-occurrence).

**Response**

```json
{
  "message": "Event deleted."
}
```

---

## Override Occurrence

Creates or replaces the `RecurrenceException` for a single occurrence of a recurring event.

```js
PUT {{host}}/api/events/{id}/occurrences/{date}
```

- `date` — the occurrence's local date in `yyyy-MM-dd`

**Request**

The overridable fields: `title`, `description`, `location`, `startsAt`, `endsAt`,
`allDay`, and `routine`. Fields not sent keep the series' values. `calendarId` and
`recurrence` cannot be overridden for one occurrence.

`PUT` here means *"the override for this date is exactly this"*: sending it again
replaces the previous override rather than merging into it.

**Response**

```json
{
  "message": "Occurrence updated."
}
```

**Errors**

- `404 NOT_FOUND` if the event is not owned by the user, or `date` is not an occurrence of it

---

## Skip Occurrence

Marks a single occurrence as skipped. The series is otherwise unchanged.

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

Converts the event into a task. The operation is **atomic**: the event is deleted and
the task created in one transaction, or neither happens.

```js
POST {{host}}/api/events/{id}/convert-to-task
```

What carries over:

| Event                                                          | Task                                                                                                                |
|----------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `title`, `description`, `calendarId`, `timeZone`, `recurrence` | same                                                                                                                |
| `createdAt`                                                    | same (the task keeps the event's creation time)                                                                     |
| `startsAt` / `endsAt`                                          | `scheduledStartAt` / `scheduledEndAt`, and `scheduledFor` is the local date of `startsAt` — unless overridden below |
| `location`                                                     | dropped (a task has no location)                                                                                    |
| `routine`                                                      | **discarded** — a task has no preparation concept                                                                   |
| occurrence overrides and skips                                 | dropped; only the series itself converts                                                                            |

The task gets a **new id**; the event's id stops existing. A recurring event becomes a
recurring task with no completions.

**Request**

```json
{
  "dueAt": "2025-07-20T17:00:00Z",
  "scheduledFor": "2025-07-19"
}
```

All fields are optional. `scheduledFor`, `scheduledStartAt`, `scheduledEndAt`, and
`dueAt` override the defaults above. For an all-day event, only `scheduledFor` is set (no time block).

**Response**

```json
{
  "message": "Event converted to task."
}
```

The task has a new id; read the tasks to get it.

---

## Bulk Delete

Deletes multiple events (whole events, including all occurrences of recurring ones) in a single request.

```js
POST {{host}}/api/events/bulk/delete
```

**Request**

```json
{
  "ids": [
    "22222222-0000-7000-8000-000000000001",
    "22222222-0000-7000-8000-000000000002"
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
    "22222222-0000-7000-8000-000000000001",
    "22222222-0000-7000-8000-000000000002"
  ],
  "notFound": []
}
```

`notFound` covers ids that do not exist or are not owned by the caller.
The endpoint never fails the whole batch because of one missing id.

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

Shifts `startsAt` and `endsAt` of every event by the same amount. For a recurring
event the whole series shifts. The routine shifts along with the event — the total
preparation duration is unchanged, so `routineStartAt` shifts by the same delta.

If any of the events is all-day, `deltaMinutes` must be a multiple of 1440, otherwise
the request fails with `400 VALIDATION_ERROR`.

This endpoint is **not safe to retry blindly**: applying it twice shifts the events
twice. If a request times out, re-read the events before repeating it.

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

- `404 NOT_FOUND` if `calendarId` is not owned by the caller
