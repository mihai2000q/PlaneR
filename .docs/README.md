# PlaneR Documentation

This folder contains the full documentation for PlaneR.
Each document is standalone, but they cross-reference each other freely.

## Structure

| Folder | Purpose |
|---|---|
| [`api/`](api/) | HTTP contract |
| [`architecture/`](architecture/) | System design, error handling, and structural decisions |
| [`dev/`](dev/) | Development workflow, conventions, and reference material |
| [`domain/`](domain/) | Domain models and business rules |
| [`features/`](features/) | What the application does, and what is planned or deferred |
| [`infrastructure/`](infrastructure/) | Database, migrations, deployment, and runtime concerns |
| [`security/`](security/) | Authentication, authorization, and threat model |
| [`testing/`](testing/) | Test strategy per platform |

## Documents

### Api

- [Api](api/Api.md) — conventions, authentication, authorization, error format, pagination
- [Api.Auth](api/controllers/Api.Auth.md) — register, login, refresh, logout, email verification, password reset
- [Api.User](api/controllers/Api.User.md) — current user profile and account
- [Api.Calendar](api/controllers/Api.Calendar.md) — calendar groupings
- [Api.Event](api/controllers/Api.Event.md) — time-bound occurrences
- [Api.Task](api/controllers/Api.Task.md) — to-do items
- [Api.Search](api/controllers/Api.Search.md) — global search across events and tasks *(sketch — not final)*

### Architecture

- [Architecture](architecture/Architecture.md) — modules, high-level flow, cross-cutting concerns
- [Error Handling](architecture/Error-Handling.md) — how errors flow from server to client

### Development

- [Git](dev/Git.md) — issues, branches, commits, pull requests, releases
- [Glossary](dev/Glossary.md) — terms used throughout the project, with their precise meanings
- [Sample Data](dev/Sample-Data.md) — seeding the database for development

### Domain

- [Domain](domain/Domain.md) — domain models, value objects, enums, task/event conversion, recurrence

### Features

- [Features](features/Features.md) — implemented, planned, deferred, and out-of-scope features

### Infrastructure

- [Database](infrastructure/Database.md) — schema, naming conventions, migrations (Flyway)
- [Deployment](infrastructure/Deployment.md) — environments, CI/CD pipelines, TLS, backups, secrets

### Security

- [Security](security/Security.md) — access control, password storage, JWT, transport security, rate limiting

### Testing

- [Testing](testing/Testing.md) — overview and shared conventions
- [Testing — Server](testing/Testing.Server.md) — unit and integration tests for the Ktor server
- [Testing — Desktop](testing/Testing.Desktop.md) — end-to-end UI tests on Compose for Desktop
- [Testing — Android](testing/Testing.Android.md) — end-to-end UI tests on Jetpack Compose