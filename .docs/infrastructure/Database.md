# PlaneR Database

The RDBMS chosen for this application infrastructure is **PostgreSQL**.

- [Naming Convention](#naming-convention)
  - [Tables](#tables)
  - [Columns](#columns)
  - [Indexes and Constraints](#indexes-and-constraints)
  - [Preparation Steps Storage](#preparation-steps-storage)
- [Migrations](#migrations)
  - [Flyway Setup](#flyway-setup)
  - [Create Migration](#create-migration)
  - [Migration Naming Convention](#migration-naming-convention)
  - [Apply Migration](#apply-migration)
  - [Apply Migration — with the connection flag](#apply-migration--with-the-connection-flag)
- [Connection Configuration](#connection-configuration)
- [Data Access](#data-access)
- [Open Questions](#open-questions)

## Naming Convention

### Tables

Tables are named in **snake_case**, in the **plural** form.

Examples:

- `users`
- `calendars`
- `events`
- `event_preparation_steps`
- `tasks`
- `task_completions`
- `recurrence_exceptions`
- `refresh_tokens`

**Why snake_case and plural?**

- PostgreSQL itself folds unquoted identifiers to lowercase, so snake_case avoids the need to double-quote every table name in raw SQL.
- Plural is the convention used by most Postgres tooling and by Flyway-generated schemas, and it reads naturally in join clauses (`SELECT * FROM users u JOIN calendars c ON ...`).

### Columns

Columns are also **snake_case**, in the **singular** form.

Examples:

- `id`
- `user_id`
- `calendar_id`
- `created_at`
- `updated_at`
- `scheduled_for`
- `scheduled_start_at`

**Foreign keys** follow the pattern `<referenced_table_singular>_id`. For example, a foreign key to `users` is named `user_id`, and a foreign key to `calendars` is named `calendar_id`.

**Timestamps** end in `_at` (`created_at`, `updated_at`, `completed_at`, `starts_at`, `ends_at`, `routine_start_at`).
**Dates** (without a time component) end in `_on` or `_for` when it reads well (`scheduled_for`), otherwise they keep a name that clearly denotes a date.

### Indexes and Constraints

- **Primary keys** are named `pk_<table>`, e.g., `pk_users`.
- **Foreign keys** are named `fk_<table>__<referenced_table>`, e.g., `fk_events__calendars`.
- **Unique constraints** are named `uq_<table>__<column>`, e.g., `uq_users__email`.
- **Indexes** are named `ix_<table>__<column>`, e.g., `ix_tasks__scheduled_for`.

Postgres-generated names for primary keys and unique constraints are acceptable if they are clear, but explicitly named constraints are preferred for consistency.

### Preparation Steps Storage

An event's **routine** — its ordered list of preparation steps — is stored in a
dedicated table, `event_preparation_steps`, rather than a JSONB column on
`events`. This keeps the steps queryable (e.g., "what is the longest preparation
step across all events?") and avoids repeating the step names and durations
inside every event row.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `event_id` | UUID | FK → `events.id`, `ON DELETE CASCADE` |
| `name` | text | 1–60 chars |
| `duration` | int | Minutes, 1–1440 |
| `position` | int | Order in the sequence, 0-based |

An index on `(event_id, position)` supports the natural read pattern of "get all
steps for this event in order":

```
ix_event_preparation_steps__event_id_position
```

`routine_start_at` on `events` is **not** a stored column. It is computed on
read from `starts_at` and the sum of the steps' durations. If the query
performance of "which events need preparation soon?" becomes a concern, a
materialized column can be added later.

## Migrations

Migrations refer to the process of modifying a database schema in a controlled and organised way.
This typically involves creating, updating, or deleting tables, columns, or other database structures
to reflect changes in the application's data model. Migrations ensure that changes to the database schema
are versioned, and can be applied consistently across different environments (development, testing, production).
They can also be rolled back at any time.

The migration tool chosen for this application is **Flyway**.
Flyway runs versioned SQL scripts at startup (or on demand) and tracks which have already been applied
in a `flyway_schema_history` table.

### Flyway Setup

Flyway is invoked through a **Gradle task** so that developers do not need to install a separate CLI.
The plugin is declared in the server module's build script:

```kotlin
plugins {
    id("org.flywaydb.flyway") version "10.x.x"
}
```

Connection details are read from the `.env` file, so the same task works for any developer
without editing the build script.

Migration files are placed under:

```
planer-server/src/main/resources/db/migration/
```

### Create Migration

Migrations are plain `.sql` files, created by hand and added to the folder above.
There is no "generate migration" command — the schema is written explicitly,
which keeps the SQL readable and reviewable.

To create a new migration, create a file following the naming convention below and add your SQL.

### Migration Naming Convention

Flyway requires the pattern:

```
V{version}__{description}.sql
```

- **version** — a monotonically increasing integer, starting at `1`
- **description** — a short, underscore-separated description of the change

Examples:

- `V1__create_users_table.sql`
- `V2__create_calendars_table.sql`
- `V3__create_events_table.sql`
- `V4__add_recurrence_rule_to_events.sql`
- `V5__add_is_default_to_calendars.sql`
- `V6__create_event_preparation_steps_table.sql`

**Version numbers are never reused.** If two developers create migrations at the same time,
the second one to merge must rename their file to the next available version.

### Apply Migration

To apply all pending migrations against the configured database, run:

```sh
./gradlew :planer-server:flywayMigrate
```

This reads the connection details from the environment and applies every migration
that has not yet been recorded in `flyway_schema_history`.

To inspect the state of the schema without changing anything:

```sh
./gradlew :planer-server:flywayInfo
```

### Apply Migration — with the connection flag

If the target database is not the one configured in the environment, Flyway accepts connection
details as task parameters:

```sh
./gradlew :planer-server:flywayMigrate \
  -Pflyway.url=jdbc:postgresql://{{database_server}}:{{database_port}}/{{database_name}} \
  -Pflyway.user={{database_user}} \
  -Pflyway.password={{database_password}}
```

By default, for local development:

- **database_server** is `localhost`
- **database_port** is `5432`
- **database_name** is `planer`
- **database_user** is `planer`
- **database_password** is `planer`

Check the `.env` file if these have been changed.

Alternatively, the `apply-database-migrations.sh` shell script can be used,
which wraps the Gradle task with the values from `.env`.

## Connection Configuration

The server reads its database connection from environment variables,
which are loaded from the `.env` file at startup.

| Variable | Purpose | Default (dev) |
|---|---|---|
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `planer` |
| `DB_USER` | Database user | `planer` |
| `DB_PASSWORD` | Database password | `planer` |

For production, these are provided by the deployment environment and never committed.
See [Security](../security/Security.md) for how other secrets (JWT signing key, etc.) are handled.

## Data Access

The specific data-access approach is **not yet decided**.
This section will document the choice when it is made.

The candidates under consideration are:

- **Exposed** — JetBrains' Kotlin-native SQL framework, type-safe DSL, no annotations, stays close to SQL.
- **jOOQ** — generates type-safe Kotlin/Java classes from the schema, heavier but very thorough.
- **Plain SQL via JDBC** — maximal control, minimal magic, more boilerplate.

Regardless of the choice, the following principles apply:

- **Migrations are the source of truth for the schema.** Any generated code (e.g., by jOOQ) is regenerated from the migrations, not the other way around.
- **Parameterised statements are mandatory.** No string concatenation in queries, ever.
- **Ownership is enforced at the query level.** Every read and write is scoped by the authenticated user's `id`.

## Open Questions

1. **ORM / query library.** Exposed, jOOQ, or plain SQL — to be decided.
2. **Value object flattening.** `RecurrenceRule` flattens onto `events` as columns; `PreparationStep` uses a dedicated table (`event_preparation_steps`). Both approaches are in use. Whether to unify them, or leave each on its own merits, is still open.
3. **UUID generation.** UUID v7 is preferred for `id` columns, but the generation site (application vs database default) is not yet fixed.
4. **Soft deletes.** Whether any entities get a `deleted_at` column instead of being hard-deleted. Currently, hard-delete is assumed everywhere.
5. **Recurrence exceptions table shape.** Whether to store exceptions as a dedicated table (`recurrence_exceptions`) or as a JSONB column on the parent event/task.
6. **Indexes for the calendar view.** The `GET /api/events?from=...&to=...` query will be the hottest read path. The `routine_start_at` value is computed rather than stored, so it can't be indexed directly — if "which events need preparation soon?" becomes a hot query, a materialized column or a generated column will be needed. The exact strategy will be decided once the query is written.