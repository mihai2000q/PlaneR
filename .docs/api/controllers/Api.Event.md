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

A time-bound occurrence. Always has a `startsAt` and `endsAt`.

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
      "title": "Team Standup",
      "description": null,
      "location": "Zoom",
      "startsAt": "2025-07-15T09:00:00Z",
      "endsAt": "2025-07-15T09:30:00Z",
      "allDay": false,
      "recurrence": {
        "frequency": "WEEKLY",
        "interval": 1,
        "byDay": ["MON", "TUE", "WED", "THU", "FRI"],
        "end": { "type": "NEVER" }
      },
      "occurrenceDate": "2025-07-15",
      "isRecurringInstance": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

`occurrenceDate` is present only for expanded occurrences of a recurring event.
It is the date this occurrence belongs to and can be used to address it in the
occurrence endpoints below.

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
  "title": "Doctor Appointment",
  "description": "Annual check-up",
  "location": "St. Mary's Clinic",
  "startsAt": "2025-07-17T08:00:00Z",
  "endsAt": "2025-07-17T09:00:00Z",
  "allDay": false,
  "recurrence": null
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

**Response**

The created event, same shape as **Get**.

**Errors**

- `400 Validation Error` if `endsAt` is not after `startsAt`
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

**Response**

```json
{
  "message": "Event updated."
}
```

**Errors**

- `400 Validation Error` if the request violates the time constraint
- `404 Not Found` if the event is not owned by the user

---

## Move

Convenience endpoint for drag-and-drop. Equivalent to calling **Update** with new times.

```js
PUT {{host}}/api/events/{id}/move
```

**Request**

```json
{
  "startsAt": "2025-07-17T10:00:00Z",
  "endsAt": "2025-07-17T11:00:00Z"
}
```

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

## Bulk Move

Moves multiple events to a new time block (same delta applied to all)
or to a different calendar.

**Option A — shift in time**

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
Useful for "push everything back by an hour".

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

**Option B — change calendar**

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