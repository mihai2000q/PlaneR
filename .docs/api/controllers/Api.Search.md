# PlaneR Api — Search

> **Status:** Sketch. The endpoints and response shape below reflect the intended
> direction, but several decisions are still open. See
> [Architecture → Open Questions](../../architecture/Architecture.md#open-questions)
> for the implementation options still under consideration.

- [Search](#search)

A single endpoint that searches across the user's events and tasks.
Search is its own controller because it queries across multiple resources and
is expected to grow independently of any single one.

---

## Search

```js
GET {{host}}/api/search?q=standup&type=all&from=2025-07-01&to=2025-12-31&page=0&size=20
```

**Query parameters**

| Param         | Required | Notes                                         |
|---------------|----------|-----------------------------------------------|
| `q`           | yes      | The search query, min 2 characters            |
| `type`        | no       | `event` \| `task` \| `all`; default `all`     |
| `calendarIds` | no       | Comma-separated list to scope the search      |
| `from`        | no       | Filter to events/tasks on or after this date  |
| `to`          | no       | Filter to events/tasks on or before this date |
| `page`        | no       | Default `0`                                   |
| `size`        | no       | Default `20`, max `50`                        |

**Response**

```json
{
  "items": [
    {
      "type": "event",
      "id": "22222222-0000-7000-8000-000000000001",
      "title": "Team Standup",
      "snippet": "...daily <em>standup</em> for the platform team...",
      "startsAt": "2025-07-15T09:00:00Z",
      "calendarId": "11111111-0000-7000-8000-000000000001"
    },
    {
      "type": "task",
      "id": "33333333-0000-7000-8000-000000000001",
      "title": "Prepare standup notes",
      "snippet": "...summarize the week for the <em>standup</em>...",
      "scheduledFor": "2025-07-15",
      "calendarId": "11111111-0000-7000-8000-000000000002"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 2,
  "totalPages": 1
}
```

**Notes**

- The `snippet` field may include lightweight highlighting (`<em>` tags).
  Whether this is done server-side or client-side is part of the open decision.
- `type` distinguishes events from tasks so the client can render them
  differently and navigate to the right detail view.
- Results are sorted by relevance, then by date as a tiebreaker.

**Errors**

- `400 VALIDATION_ERROR` if `q` is missing or shorter than 2 characters
