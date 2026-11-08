# PlaneR Architecture

This document describes the shape of the PlaneR system at a high level.
It is intentionally light on specifics — folder layouts, class names, and
framework choices on the server side are left to be decided later and
documented as they are settled.

- [Overview](#overview)
- [Modules](#modules)
- [The Shared Module](#the-shared-module)
- [Client Applications](#client-applications)
- [Server](#server)
- [Communication Between Client and Server](#communication-between-client-and-server)
- [Offline and Sync](#offline-and-sync)
- [Cross-Cutting Concerns](#cross-cutting-concerns)
- [Open Questions](#open-questions)

## Overview

PlaneR is a personal time-planning application. It follows a **client–server** model:

- A **Ktor server** owns the data and the business rules.
- **Desktop** and **Android** clients are separate applications. Each renders its own UI and talks to the server over
  HTTP.
- A **shared** module holds the non-visual code that both clients (and, partly, the server) can reuse: domain models,
  DTOs, validation, and the networking client.

The system has three properties that drive most of the architecture:

1. **Single-user.** No sharing, no collaboration, no multi-tenancy. This removes a large class of problems (permission
   matrices, real-time sync between users, conflict resolution across users).
2. **Separate clients, shared contract.** Desktop (Windows, macOS, Linux via the JVM) and Android each have their own
   UI, written against their platform's toolkit. Android is a native app; the desktop client uses Compose for Desktop,
   which runs on the JVM and draws its own UI rather than using the operating system's widgets. What the clients share
   is the *contract* with the server — the models, DTOs, validation, and API client — so they cannot drift apart on what
   an `Event` is. iOS is planned as a future native sibling client.
3. **Calendar-shaped.** Most operations are reads over a date range and simple writes of individual items. This favours
   a straightforward request–response API over anything more elaborate.

## Modules

The repository is a multi-module **Gradle** build with four modules. Only `planer-shared` is a **Kotlin Multiplatform**
module; the others are plain JVM or Android modules.

| Module           | Platform                      | Purpose                                                               |
|------------------|-------------------------------|-----------------------------------------------------------------------|
| `planer-shared`  | KMP (JVM, Android; iOS later) | Domain models, DTOs, validation, networking client. No UI.            |
| `planer-server`  | JVM                           | Ktor HTTP server, persistence, authentication                         |
| `planer-desktop` | JVM                           | Desktop application: UI (Compose for Desktop), view models, packaging |
| `planer-android` | Android                       | Android application: UI (Jetpack Compose), view models, packaging     |

A dependency rule holds throughout:

- **`planer-shared`** depends on nothing else in the repository. It is the foundation.
- **`planer-server`** depends on `planer-shared` for the shared domain models, DTOs, and validation rules. It does not
  use the networking client.
- **`planer-desktop`** and **`planer-android`** depend on `planer-shared`. They do **not** depend on each other, and
  they do **not** depend on `planer-server`.

In other words: everything depends on `planer-shared`, nothing depends on `planer-server` except the deployment
pipeline.

Because `planer-shared` also contains the networking client, the server's dependency graph includes the Ktor client
library even though it never calls it. This is accepted for now; if it becomes a nuisance, the client can be split out
into its own module without changing anything else.

If iOS is added in the future, it becomes a fifth module (`planer-ios`) that depends on `planer-shared` like the other
clients, with a native UI of its own. It does not touch `planer-android` or `planer-desktop`.

## The Shared Module

`planer-shared` is the shared contract between the server and the clients. It contains:

### Domain models

The shapes described in [Domain](../domain/Domain.md) — `User`, `Calendar`, `Event`, `Task`, `RecurrenceRule`,
`PreparationStep` — as plain Kotlin data classes, free of framework annotations. They are the in-memory representation
used by the server's business logic and by the clients.

### DTOs

Request and response bodies for the API described in [Api](../api/Api.md), serialized with kotlinx.serialization.
Keeping them in `planer-shared` means both client and server compile against the same wire contract. DTOs are separate
from the domain models: the wire format includes fields the domain does not (for example `occurrenceDate` on expanded
recurring events), and the two are allowed to diverge.

### Validation

Rules like "title is 1–120 characters", "endsAt must be after startsAt", "color matches `^#[0-9A-Fa-f]{6}$`", "a
preparation step's duration is between 1 and 1440 minutes". Validating on both client (for immediate UI feedback) and
server (as the source of truth) using the same code eliminates drift between the two.

### Networking client

A thin HTTP client that both clients use to talk to the server. It wraps the endpoints defined in [Api](../api/Api.md)
and returns typed responses. It also:

- attaches the `X-Client-Platform` and `X-Client-Version` headers to every request
- injects the access token and refreshes it once on `401 UNAUTHORIZED`
- maps non-2xx responses to a shared `ApiError` type (see [Error Handling](Error-Handling.md))
- signals `UPGRADE_REQUIRED` so the app can show its forced-update screen

Token *storage* is not part of the client: it is platform-specific (Android Keystore, the desktop OS keychain) and is
provided by each app through an interface that `planer-shared` defines.

### What Shared does not contain

**No UI and no view models.** Screens, components, navigation, state holders, and anything that depends on a UI toolkit
live in the client modules. This is deliberate: it keeps `planer-shared` usable from the server, and it lets each client
change its UI without touching the other.

## Client Applications

Each client owns its entire UI layer. The two clients are structurally similar but share no UI code; where a screen
exists on both, it is implemented twice, against the same `planer-shared` contract.

### Desktop

A JVM application built with **Compose for Desktop**. It provides:

- The screens, components, navigation, and view models
- A native window with platform-appropriate title bar, menu, and icon
- Native file dialogs, tray icons, and other desktop-only integrations
- Desktop secure storage for the refresh token (OS keychain)
- Packaging for Windows (MSI), macOS (DMG), and Linux (DEB)

### Android

An Android application built with **Jetpack Compose**. It provides:

- The screens, components, navigation, and view models
- A single `MainActivity` hosting the Compose UI
- Android-specific integrations: notifications, permissions, background work
- Android Keystore–backed storage for the refresh token
- Packaging as APK / AAB

### iOS (planned)

If iOS support is added, it becomes a **sibling module** (`planer-ios`) with a native UI (SwiftUI) that consumes
`planer-shared` through Kotlin Multiplatform's iOS target. `planer-shared` would gain an `iosMain` source set for any
`expect`/`actual` implementations it needs. `planer-android` and `planer-desktop` would not change.

## Server

The server is a **Ktor** application running on the JVM. Its responsibilities are:

- **Authentication** — registration, login, token issuance and refresh, email verification, password reset
- **Authorization** — every request is scoped to the authenticated user
- **Persistence** — reading and writing the domain models
- **Business logic** — recurrence expansion, routine layout, task/event conversion, cascade rules on deletion
- **HTTP surface** — the endpoints described in [Api](../api/Api.md)

The internal structure of the server (how requests flow from a route to the database, what layers or packages exist,
whether use cases are explicit or implicit) is **deliberately not decided yet** and will be documented separately once
the codebase takes shape.

### Persistence

Data is stored in **PostgreSQL**, accessed through **Exposed**. Migrations are versioned with **Flyway** and applied
by a separate step that runs before the server starts (Gradle in development, a one-shot container in Docker).
See [Database](../infrastructure/Database.md) for the schema, naming conventions, and migration workflow.

### Computed timelines

The server does two kinds of computation that would otherwise have to be
duplicated on every client:

- **Recurrence expansion** — recurring events and tasks are stored **once**, with a `RecurrenceRule` and a `timeZone`.
  When a client requests items in a date range (at most 366 days), the server expands the rule, in that timezone, into
  concrete occurrences before returning them.
- **Derived values** — streak, focus now, today's progress, overdue count, and the next routine start are computed in
  one place and served by the [Summary](../api/controllers/Api.Summary.md) endpoint.
- **Routine layout** — an event with preparation steps carries a derived `routineStartAt` and, per step, a computed
  `startsAt`/`endsAt`. The server lays the steps out backward from the event's `startsAt`, so every client renders the
  same timeline without reimplementing the arithmetic.

This is where the server earns its keep. Because the clients no longer share any UI
code, this matters even more: the timeline is computed once, in one place, and
shipped as data, so two independently written UIs always agree on it.

## Communication Between Client and Server

The protocol is **HTTP/JSON**, request–response, authenticated with a JWT bearer token.

- **Stateless per request.** The server does not maintain session state. Every request carries the token and is answered
  independently.
- **Not versioned, but gated.** There is no `/v1/` prefix. Instead, every request carries `X-Client-Platform` and
  `X-Client-Version`, and the server answers `426 Upgrade Required` to clients below a configured minimum. Contract
  changes should be additive where possible; the minimum is raised only for breaking changes, and only once the new
  client is available to users. See [Api → Versioning and Forced Updates](../api/Api.md#versioning-and-forced-updates).
- **Errors follow a single shape.** A JSON body with `status`, `code`, `message`, and optional `errors`
  (see [Api](../api/Api.md)).
- **Timestamps are always UTC.** The client renders them in the user's timezone, which is stored on the `User`. Events
  and tasks additionally carry their own `timeZone` so recurrence stays correct across daylight-saving changes
  (see [Domain → Timezones](../domain/Domain.md#timezones)).

There is **no WebSocket, no server push, and no real-time collaboration**. The client polls when it needs fresh data,
and only for the visible date range. This is sufficient for a personal planner and keeps the server simple.

## Offline and Sync

Offline support is **not part of v1**. The client assumes the server is reachable.

A future version may add a local cache so that recent data is visible without a network connection, along with a queue
for pending writes. If that happens, the sync model will most likely be:

- **Last-write-wins per field**, with `updatedAt` as the tiebreaker.
- **No conflict resolution UI** — a personal planner rarely has genuine conflicts, and the simplicity is worth more than
  correctness in the edge cases.

This is noted here only to explain why the domain model already carries `updatedAt` on every entity. The client is free
to ignore it in v1.

## Cross-Cutting Concerns

| Concern            | Where it lives                                                                                                                                                                        |
|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Authentication** | Server. Issues and validates JWTs.                                                                                                                                                    |
| **Authorization**  | Server. Every query is scoped by the token's `sub` claim.                                                                                                                             |
| **Validation**     | Both. `planer-shared` provides the rules; server re-validates as the source of truth.                                                                                                 |
| **Error handling** | Server catches and shapes errors; `planer-shared` maps them to a typed `ApiError`; each client turns that into UI.                                                                    |
| **Forced updates** | Server enforces the minimum client version; `planer-shared` surfaces `UPGRADE_REQUIRED`; each client shows the blocking update screen.                                                |
| **Timezones**      | `User.timezone` defines "today" and the default for new items; each item's own `timeZone` anchors its recurrence. The client sends UTC instants; the server never assumes a timezone. |
| **Recurrence**     | Server expands rules into occurrences. `planer-shared` never re-implements the expansion.                                                                                             |
| **Routines**       | Server computes `routineStartAt` and per-step times. Clients render them.                                                                                                             |
| **Token storage**  | Each client, using the platform's secure storage behind an interface defined in `planer-shared`.                                                                                      |
| **Localization**   | Not in v1. The UI is English-only.                                                                                                                                                    |

## Open Questions

These are things the architecture does not commit to yet, and that will be settled as the codebase grows.

### Server

1. **Server internal layout.** Whether to use explicit use cases, whether to split into named layers, how far to
   abstract the data access.
2. ~~**Database schema and ORM.**~~ **Resolved:** Exposed (DSL), with the schema owned by Flyway migrations.
   See [Database](../infrastructure/Database.md#data-access).

### Authentication

3. ~~**Unverified user capabilities.**~~ **Resolved:** full use, except changing the email.
   See [Security](../security/Security.md#email-verification).
4. ~~**Verification and reset token storage.**~~ **Resolved:** opaque random tokens, stored hashed in `one_time_tokens`.
   See [Security](../security/Security.md#one-time-tokens).
5. ~~**Token expiry and cleanup.**~~ **Resolved:** verification 24 h, reset 1 h, cleaned up by a daily job.
   See [Security](../security/Security.md#one-time-tokens).
6. **Email delivery mechanism.** SMTP directly, or a transactional email service (Resend, Postmark, SendGrid). Affects
   deliverability, cost, and how much code lives in the server.
7. ~~**Rate limiting on auth endpoints.**~~ **Resolved:** limits proposed
   in [Security](../security/Security.md#rate-limiting), using Ktor's `RateLimit` plugin; thresholds to be tuned from
   real usage.

### Search

8. **Search implementation.** Postgres full-text search, trigram matching, or a dedicated engine (Meilisearch,
   ElasticSearch, Typesense). Postgres is simplest; a dedicated engine scales better and supports richer ranking.
   See [Api.Search](../api/controllers/Api.Search.md) for the API surface this needs to support.
9. **Search scope and ranking.** Which fields are indexed (title, description, location), how results are ranked, and
   whether snippets are generated server-side.

### Operations

10. **Logging strategy.** What gets logged, at what levels, in what format, and where it goes. Structured JSON to stdout
    is the modern default; the specifics depend on where the server is deployed.
11. **Observability.** Whether anything beyond logs is needed — metrics, traces, uptime monitoring. Not urgent for a
    personal app, but worth deciding before the first production issue.
12. **Backups.** Database backup strategy and retention. Partially addressed
    in [Deployment](../infrastructure/Deployment.md#backups); specifics TBD.

### Future Features

13. **Notifications.** General reminders are deferred: they would need a `Reminder` model, a scheduler, and
    platform-specific code. The one planned exception is
    the [routine start reminder](../features/Features.md#routine-start-reminder), a *local* notification each client
    schedules from data it already has, which needs no server-side component.
14. **Routine templates.** A reusable `Routine` model that can be applied to an event with one click. Deferred to a
    future version; in v1 the routine lives directly on the event.
15. **Offline support.** Whether to add a local cache in a future version and what shape it would take.
16. **iOS.** Whether and when to add `planer-ios` as a sibling client, and what platform-specific integrations it would
    need.
17. **Deployment topology.** Where the server runs, how TLS is terminated, whether a CDN fronts anything. Partially
    addressed in [Deployment](../infrastructure/Deployment.md); the cloud environment specifics are TBD.

### Clients

18. **Client-side state management.** Each client picks its own pattern for view models and state (for example,
    `ViewModel` + `StateFlow` on Android). Without shared view models, consider whether the two clients should at least
    follow the same conventions, documented here.
19. **Client configuration.** How each client learns the server URL in release builds (build-time constant, settings
    screen, or both), now that `.env` files only make sense in development.
