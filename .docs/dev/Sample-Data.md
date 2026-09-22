# PlaneR Sample Data

Sample data is a set of SQL seed files that populate the database with realistic
content for development and manual testing. It lets you open the desktop or
Android app and see a fully populated calendar instead of an empty one.

- [Overview](#overview)
- [Files](#files)
- [Usage](#usage)
- [Resetting](#resetting)
- [Adding New Data](#adding-new-data)

## Overview

The sample data is designed to cover the common cases the UI needs to render:

- Multiple calendars with different colors and icons
- Events that are today, this week, and this month
- Tasks that are scheduled, unscheduled, due, overdue, and completed
- A recurring event (daily standup)
- A recurring task (water plants weekly)
- A user with a populated streak (several consecutive days of completions)

The seed is deliberately small — just enough to exercise every screen without
drowning the developer in irrelevant rows.

## Files

Sample data lives at the repository root in `sample-data/`:

```
sample-data/
├── 01-users.sql
├── 02-calendars.sql
├── 03-events.sql
└── 04-tasks.sql
```

Files are numbered so they run in dependency order (users → calendars → events → tasks).

Each file uses `INSERT ... ON CONFLICT DO NOTHING` so it is safe to re-run.

## Usage

To load all sample data into the running database:

```sh
./scripts/add-sample-data.sh
```

The script reads the connection details from `.env` and applies each file in order.

To load only one file:

```sh
psql "$DATABASE_URL" -f sample-data/02-calendars.sql
```

## Resetting

To wipe the database and reload from scratch:

```sh
./scripts/reset-database.sh
```

This runs Flyway `clean` (drops everything), reapplies migrations, and reloads
the sample data. It is **destructive** and will only work in development — the
script refuses to run against a database whose name does not match the pattern
`planer*`.

## Adding New Data

When you add a new entity to the domain and want it represented in the seed:

1. Add rows to the appropriate file, following the existing style.
2. Keep IDs stable across files so foreign keys resolve.
3. Use fixed UUIDs (not `gen_random_uuid()`) so reruns are idempotent.
4. Test by running `reset-database.sh` and confirming the app renders everything.

If the new entity introduces a new dependency (e.g., `refresh_tokens` before
events), create a new numbered file rather than editing an existing one — the
numbering is the execution order.