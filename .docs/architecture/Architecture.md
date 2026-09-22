# PlaneR Architecture

This document describes the shape of the PlaneR system at a high level.
It is intentionally light on specifics — folder layouts, class names, and
framework choices on the server side are left to be decided later and
documented as they are settled.

- [Overview](#overview)
- [Modules](#modules)
- [The Core Module](#the-core-module)
- [Client Applications](#client-applications)
- [Server](#server)
- [Communication Between Client and Server](#communication-between-client-and-server)
- [Offline and Sync](#offline-and-sync)
- [Cross-Cutting Concerns](#cross-cutting-concerns)
- [Open Questions](#open-questions)

## Overview

PlaneR is a personal time-planning application. It follows a **client–server** model:

- A **Ktor server** owns the data and the business rules.
- **Desktop** and **Android** clients render the UI and talk to the server over HTTP.
- A shared **core** module holds everything that can be reused by both clients: domain models, DTOs, validation, and UI components.

The system has three properties that drive most of the architecture:

1. **Single-user.** No sharing, no collaboration, no multi-tenancy. This removes a large class of problems (permission matrices, real-time sync between users, conflict resolution across users).
2. **Multi-platform.** Desktop (Windows, macOS, Linux via JVM) and Android must share as much code as possible. iOS is planned as a future sibling client.
3. **Calendar-shaped.** Most operations are reads over a date range and simple writes of individual items. This favours a straightforward request–response API over anything more elaborate.

## Modules

The repository is organised as a **Kotlin Multiplatform** project with four Gradle modules:

| Module | Platform | Purpose |
|---|---|---|
| `planer-core` | KMP (common) | Domain models, DTOs, validation, shared UI components, networking client |
| `planer-server` | JVM | Ktor HTTP server, persistence, authentication |
| `planer-desktop` | JVM | Desktop entry point, packaging |
| `planer-android` | Android | Android entry point, packaging |

A dependency rule holds throughout:

- **`planer-core`** depends on nothing else in the repository. It is the foundation.
- **`planer-server`** depends on `planer-core` for shared domain models and DTOs.
- **`planer-desktop`** and **`planer-android`** depend on `planer-core`. They do **not** depend on each other, and they do **not** depend on `planer-server`.

In other words: everything depends on `planer-core`, nothing depends on `planer-server` except the deployment pipeline.

If iOS is added in the future, it becomes a fifth module (`planer-ios`) that depends on `planer-core` like the other clients. It does not touch `planer-android` or `planer-desktop`.

## The Core Module

`planer-core` is the heart of the client side. It contains:

### Domain models

The shapes described in [Domain](../domain/Domain.md) — `User`, `Calendar`, `Event`, `Task` — as plain Kotlin data classes. These are the *shared* representation used on both sides of the wire.

### DTOs

Request and response bodies for the API described in [Api](../api/Api.md).
Keeping them in `planer-core` means both client and server compile against the same contract.

### Validation

Rules like "title is 1–120 characters", "endsAt must be after startsAt", "color matches `^#[0-9A-Fa-f]{6}$`". Validating on both client (for immediate UI feedback) and server (as the source of truth) using the same code eliminates drift between the two.

### Shared UI

The screens, components, view models, and navigation live here, written with **Compose Multiplatform**. The `planer-desktop` and `planer-android` modules only provide the platform-specific entry point and configuration.

### Networking client

A thin HTTP client that both platforms use to talk to the server. It wraps the endpoints defined in `Api.md` and returns typed responses.

## Client Applications

### Desktop

A JVM application built with **Compose for Desktop**. It provides:

- A native window with platform-appropriate title bar, menu, and icon
- Native file dialogs, tray icons, and other desktop-only integrations
- Packaging for Windows (MSI), macOS (DMG), and Linux (DEB)

Everything else — screens, state, networking — is inherited from `planer-core`.

### Android

An Android application built with **Jetpack Compose**. It provides:

- A single `MainActivity` that hosts the shared UI
- Android-specific integrations: notifications, permissions, background work
- Packaging as APK / AAB

Everything else — screens, state, networking — is inherited from `planer-core`.

### iOS (planned)

If iOS support is added, it becomes a **sibling module** (`planer-ios`) that depends on `planer-core` alongside the other clients. The shared UI in `planer-core` would gain an `iosMain` source set for any `expect`/`actual` implementations iOS needs. `planer-android` would not change.

## Server

The server is a **Ktor** application running on the JVM. Its responsibilities are:

- **Authentication** — registration, login, token issuance and refresh, email verification, password reset
- **Authorization** — every request is scoped to the authenticated user
- **Persistence** — reading and writing the domain models
- **Business logic** — recurrence expansion, task/event conversion, cascade rules on deletion
- **HTTP surface** — the endpoints described in [Api](../api/Api.md)

The internal structure of the server (how requests flow from a route to the database, what layers or packages exist, whether use cases are explicit or implicit) is **deliberately not decided yet** and will be documented separately once the codebase takes shape.

### Persistence

Data is stored in **PostgreSQL**. Migrations are versioned with **Flyway** and applied at startup.
See [Database](../infrastructure/Database.md) for the schema, naming conventions, and migration workflow.

### Recurrence expansion

Recurring events and tasks are stored **once**, with a `RecurrenceRule`. When a client requests events in a date range, the server expands the rule into concrete occurrences before returning them. This is the one place where the server does meaningful computation rather than just relaying data.

## Communication Between Client and Server

The protocol is **HTTP/JSON**, request–response, authenticated with a JWT bearer token.

- **Stateless per request.** The server does not maintain session state. Every request carries the token and is answered independently.
- **Versioned implicitly by shape.** If the contract changes, both sides are updated in lockstep — this is a single-repository, single-team project, so no formal API versioning is planned for now.
- **Errors follow a single shape.** A JSON body with `status`, `code`, `message`, and optional `errors` (see [Api](../api/Api.md)).
- **Timestamps are always UTC.** The client renders them in the user's timezone, which is stored on the `User`.

There is **no WebSocket, no server push, and no real-time collaboration**. The client polls when it needs fresh data, and only for the visible date range. This is sufficient for a personal planner and keeps the server simple.

## Offline and Sync

Offline support is **not part of v1**. The client assumes the server is reachable.

A future version may add a local cache so that recent data is visible without a network connection, along with a queue for pending writes. If that happens, the sync model will most likely be:

- **Last-write-wins per field**, with `updatedAt` as the tiebreaker.
- **No conflict resolution UI** — a personal planner rarely has genuine conflicts, and the simplicity is worth more than correctness in the edge cases.

This is noted here only to explain why the domain model already carries `updatedAt` on every entity. The client is free to ignore it in v1.

## Cross-Cutting Concerns

| Concern | Where it lives |
|---|---|
| **Authentication** | Server. Issues and validates JWTs. |
| **Authorization** | Server. Every query is scoped by the token's `sub` claim. |
| **Validation** | Both. `planer-core` provides the rules; server re-validates as the source of truth. |
| **Error handling** | Server catches and shapes errors; `planer-core` maps them to UI-friendly messages. |
| **Timezones** | The `User.timezone` is authoritative. The client sends UTC instants; the server never assumes a timezone. |
| **Recurrence** | Server expands rules into occurrences. `planer-core` never re-implements the expansion. |
| **Localization** | Not in v1. The UI is English-only. |

## Open Questions

These are things the architecture does not commit to yet, and that will be settled as the codebase grows.

### Server

1. **Server internal layout.** Whether to use explicit use cases, whether to split into named layers, how far to abstract the data access.
2. **Database schema and ORM.** Whether to use Exposed, jOOQ, or plain SQL. Will be documented in `Database.md` when it is chosen.

### Authentication

3. **Unverified user capabilities.** What an unverified user is allowed to do. Options range from "nothing except verify" to "everything except email change." To be finalised when the verification flow is built.
4. **Verification and reset token storage.** Whether tokens live in a dedicated table, are signed JWTs, or are stored as hashed opaque strings. Each option has different revocation and cleanup properties.
5. **Token expiry and cleanup.** How long verification and password reset tokens live, and whether expired rows are cleaned up periodically or left to accumulate.
6. **Email delivery mechanism.** SMTP directly, or a transactional email service (Resend, Postmark, SendGrid). Affects deliverability, cost, and how much code lives in the server.
7. **Rate limiting on auth endpoints.** Login, register, forgot-password, and resend-verification all need protection against abuse. Ktor has a `RateLimit` plugin; the limits themselves are undecided.

### Search

8. **Search implementation.** Postgres full-text search, trigram matching, or a dedicated engine (Meilisearch, ElasticSearch, Typesense). Postgres is simplest; a dedicated engine scales better and supports richer ranking. See [Api.Search](../api/controllers/Api.Search.md) for the API surface this needs to support.
9. **Search scope and ranking.** Which fields are indexed (title, description, location), how results are ranked, and whether snippets are generated server-side.

### Operations

10. **Logging strategy.** What gets logged, at what levels, in what format, and where it goes. Structured JSON to stdout is the modern default; the specifics depend on where the server is deployed.
11. **Observability.** Whether anything beyond logs is needed — metrics, traces, uptime monitoring. Not urgent for a personal app, but worth deciding before the first production issue.
12. **Backups.** Database backup strategy and retention. Partially addressed in [Deployment](../infrastructure/Deployment.md#backups); specifics TBD.

### Future Features

13. **Notifications.** Whether to support reminders, how they are scheduled, and how they reach the user on desktop and Android. Would require a `Reminder` model, a scheduler, and platform-specific code.
14. **Offline support.** Whether to add a local cache in a future version and what shape it would take.
15. **iOS.** Whether and when to add `planer-ios` as a sibling client, and what platform-specific integrations it would need.
16. **Deployment topology.** Where the server runs, how TLS is terminated, whether a CDN fronts anything. Partially addressed in [Deployment](../infrastructure/Deployment.md); the cloud environment specifics are TBD.