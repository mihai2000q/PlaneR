# PlaneR Api — Calendar

- [Get Collection](#get-collection)
- [Get](#get)
- [Create](#create)
- [Update](#update)
- [Set Default](#set-default)
- [Reorder](#reorder)
- [Delete](#delete)

A Calendar is a coloured grouping for events and tasks.
Every user has exactly one default calendar, created on registration. The default
calendar cannot be deleted; to get rid of it, make another calendar the default first.

---

## Get Collection

```js
GET {{host}}/api/calendars
```

Not paginated — a user typically has fewer than 20 calendars.

**Response**

```json
{
  "calendars": [
    {
      "id": "11111111-0000-7000-8000-000000000001",
      "name": "Work",
      "color": "#7C3AED",
      "icon": "BRIEFCASE",
      "position": 0,
      "isDefault": false,
      "itemCount": 8
    },
    {
      "id": "11111111-0000-7000-8000-000000000002",
      "name": "Personal",
      "color": "#F59E0B",
      "icon": "STAR",
      "position": 1,
      "isDefault": true,
      "itemCount": 5
    }
  ],
  "totalCount": 2
}
```

`itemCount` is the number of events and tasks assigned to the calendar.

---

## Get

```js
GET {{host}}/api/calendars/{id}
```

- `id` — 36-char UUID

**Response**

```json
{
  "id": "11111111-0000-7000-8000-000000000001",
  "name": "Work",
  "color": "#7C3AED",
  "icon": "BRIEFCASE",
  "position": 0,
  "isDefault": false,
  "createdAt": "2025-07-15T12:00:00Z",
  "updatedAt": "2025-07-15T12:00:00Z"
}
```

---

## Create

```js
POST {{host}}/api/calendars
```

**Request**

```json
{
  "name": "Health",
  "color": "#10B981",
  "icon": "HEALTH"
}
```

| Field | Required | Notes |
|---|---|---|
| `name` | yes | 1–40 chars, trimmed, unique per user (case-insensitive: "work" and "Work" collide) |
| `color` | yes | Hex string matching `^#[0-9A-Fa-f]{6}$` |
| `icon` | yes | Value from `CalendarIcon` enum |

New calendars are appended to the end of the sidebar (`position = max + 1`).

**Response**

The created calendar, same shape as **Get**.

**Errors**

- `409 Conflict` if a calendar with the same name already exists for this user

---

## Update

```js
PATCH {{host}}/api/calendars/{id}
```

**Request**

```json
{
  "name": "Wellness",
  "color": "#22C55E",
  "icon": "FITNESS"
}
```

All fields are optional; only provided fields are updated.

**Response**

The updated calendar, same shape as **Get**.

**Errors**

- `409 CONFLICT` if the new `name` collides with another calendar of this user

---

## Set Default

Makes this calendar the user's default. The previous default stops being the
default in the same operation, so there is always exactly one.

```js
PUT {{host}}/api/calendars/{id}/default
```

**Response**

The updated calendar, same shape as **Get**.

---

## Reorder

Changes the calendar's position in the sidebar.

```js
PUT {{host}}/api/calendars/{id}/position
```

**Request**

```json
{
  "newPosition": 2
}
```

The server shifts the positions of the calendars between the old and new positions.

**Response**

```json
{
  "message": "Position updated."
}
```

---

## Delete

```js
DELETE {{host}}/api/calendars/{id}
```

**Query parameters**

| Param | Values | Default | Meaning |
|---|---|---|---|
| `mode` | `unassign` \| `reassign` \| `cascade` | `unassign` | What happens to events and tasks in this calendar |
| `targetCalendarId` | UUID | — | Required when `mode=reassign` |

- `unassign` — events and tasks keep existing with `calendarId = null`
- `reassign` — events and tasks are moved to `targetCalendarId`
- `cascade` — events and tasks are deleted along with the calendar

**Response**

```json
{
  "message": "Calendar deleted.",
  "movedItems": 12,
  "deletedItems": 0
}
```

**Errors**

- `400 VALIDATION_ERROR` if the user tries to delete the default calendar (set another one as default first)
- `400 VALIDATION_ERROR` if `mode=reassign` and `targetCalendarId` is missing or invalid