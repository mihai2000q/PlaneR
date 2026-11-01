# PlaneR Api — Summary

- [Get Summary](#get-summary)

A single read-only endpoint that returns the **derived values** the home screen shows:
streak, focus now, today's progress, overdue count, and the next routine start.

These are defined in [Domain → Derived Concepts](../../domain/Domain.md#derived-concepts).
They are computed on the server, in the user's timezone, so that the Desktop and Android
clients — which share no code beyond the contract — always show the same numbers.

Per-calendar counts are not here; they are the `itemCount` of each calendar in
[Api.Calendar](Api.Calendar.md#get-collection).

---

## Get Summary

```js
GET {{host}}/api/summary
```

"Today" and "now" are taken from the server clock and the user's timezone.

**Response**

```json
{
  "today": "2025-07-15",
  "streakDays": 6,
  "todayProgress": { "completed": 3, "total": 7 },
  "overdueCount": 2,
  "focusNow": {
    "type": "event",
    "id": "22222222-0000-7000-8000-000000000001",
    "title": "Meet friends",
    "startsAt": "2025-07-15T17:30:00Z",
    "occurrenceDate": null
  },
  "nextRoutine": {
    "eventId": "22222222-0000-7000-8000-000000000001",
    "title": "Meet friends",
    "routineStartAt": "2025-07-15T15:30:00Z",
    "occurrenceDate": null
  }
}
```

| Field           | Meaning                                                                                                                                                               |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `today`         | The current date in the user's timezone                                                                                                                               |
| `streakDays`    | Consecutive days, ending today or yesterday, with at least one completed task. A day with no completion yet today does not break a streak that ran through yesterday. |
| `todayProgress` | Tasks scheduled for today: how many are completed, out of how many                                                                                                    |
| `overdueCount`  | Tasks with `dueAt` in the past that are not completed                                                                                                                 |
| `focusNow`      | The next upcoming event or scheduled task (`type` is `event` or `task`), or `null` if there is none                                                                   |
| `nextRoutine`   | The upcoming event whose routine starts next and has not started yet, or `null`. Intended for the "start preparing at 15:30" card.                                    |

`occurrenceDate` is set when the item is an occurrence of a recurring event or task.

The endpoint takes no parameters. The Overdue, My Day, and Completed *lists* come from
[Api.Task](Api.Task.md#get-collection) with the `overdue`, range, and `completed` filters.
