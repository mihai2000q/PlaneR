# PlaneR Server

- [PlaneR Server](#planer-server)
    - [Abstract](#abstract)
    - [Prerequisites](#prerequisites)
        - [Kotlin and JDK](#kotlin-and-jdk)
        - [Docker](#docker)
        - [Http Client](#http-client)
    - [Get Started](#get-started)
        - [Environment Variables](#environment-variables)
        - [Docker Containers](#docker-containers)
        - [Restore Dependencies](#restore-dependencies)
        - [Run](#run)
        - [Quick Test](#quick-test)
    - [Sample Data](#sample-data)
    - [Build Executable](#build-executable)
    - [Project Structure](#project-structure)
    - [Architecture](#architecture)
    - [Api](#api)
        - [Authentication](#authentication)
    - [Database](#database)
        - [Migration](#migration)
    - [Testing](#testing)
        - [Unit Testing](#unit-testing)
        - [Integration Testing](#integration-testing)
    - [Live Reload](#live-reload)

## Abstract

This is the HTTP Server (or backend) of the **PlaneR** application, written completely in Kotlin using the **Ktor**
framework.

It owns the data and the business rules: authentication, authorization, persistence, and everything the clients rely on.
The internal structure of the module is deliberately left open and will be documented as the codebase takes shape. The
high-level responsibilities and request flow are described in the [Architecture section](#architecture) below.

> **Current status: skeleton.** Today the server starts, serves `GET /health`, and has one test. Everything else in this
> README — the database, migrations, authentication, the Docker setup, the `.env` file, the sample-data scripts —
> describes the **planned** setup and does not exist yet. Sections that depend on something not yet built say so.

## Prerequisites

Before you can get started, there are some things you need to have installed on your system.

### Kotlin and JDK

The application is written in Kotlin and runs on the JVM, so you need the **JDK 21 or later**. If you don't have it
already installed, please download it from here: `https://adoptium.net/`.

You also need the Kotlin compiler, but if you use the Gradle wrapper included in the project, it will handle the Kotlin
version automatically.

### Docker

Developing becomes easier while using isolated containers provided by Docker, so, if you do not have it installed
already, please do so from here: `https://www.docker.com/products/docker-desktop/` (you may have to restart afterward).

### Http Client

To exercise the API manually, you will want an HTTP client. Any of the following work:

- **Postman** — `https://www.postman.com/downloads/`
- **Insomnia** — `https://insomnia.rest/download`
- **HTTPie** — `https://httpie.io/cli`
- **curl** — preinstalled on most systems

The examples in this README use the Postman `{{host}}` variable convention, but any client works the same way.

## Get Started

To get started on contributing to this project, you can do so by following the next steps.

### Environment Variables

> **Partly built.** `.env.dev` exists and is used by `docker-compose.yml`, but the server currently reads only
> `SERVER_PORT`. The rest of this section is the planned configuration.

Create a `.env` file by copying the provided `.env.dev` file. This file contains the variables needed for the database
container initialization and the application configuration.

The required variables are:

| Variable                     | Purpose                                                                                 | Default (dev)                   |
|------------------------------|-----------------------------------------------------------------------------------------|---------------------------------|
| `DB_HOST`                    | Database host                                                                           | `localhost`                     |
| `DB_PORT`                    | Database port                                                                           | `5432`                          |
| `DB_NAME`                    | Database name                                                                           | `planer`                        |
| `DB_USER`                    | Database user                                                                           | `planer`                        |
| `DB_PASSWORD`                | Database password                                                                       | `planer`                        |
| `JWT_SECRET_KEY`             | Signing key for tokens, min 32 bytes                                                    | *(set manually — see Security)* |
| `SERVER_PORT`                | Port the server listens on                                                              | `8080`                          |
| `MIN_CLIENT_VERSION_ANDROID` | Minimum Android app version; older clients get `426`. Leave unset to disable the check. | *(unset)*                       |
| `MIN_CLIENT_VERSION_DESKTOP` | Minimum Desktop app version; same behaviour                                             | *(unset)*                       |

See [Security](../.docs/security/Security.md) for the requirements on `JWT_SECRET_KEY` and
the [Database doc](../.docs/infrastructure/Database.md#connection-configuration) for details on the connection
variables.

### Docker Containers

A `docker-compose.yml` file at the repository root starts the whole backend. It reads variables from `.env` and starts:

- the **database** container (Postgres), with a healthcheck
- the **server** container, built from `planer-server/Dockerfile`, which waits for the database to be healthy

From the repository root, copy the environment file and start the containers:

```sh
cp .env.dev .env
docker compose up -d
```

Check that everything is healthy with `docker compose ps`. To stop, run `docker compose down` (add `-v` to also delete
the database volume).

If you want to run only the database and let the server run from your IDE, use:

```sh
docker compose up -d database
```

The database starts empty. With `docker compose up -d`, a one-shot `migrate` container applies all pending Flyway
migrations before the server starts. **The server does not migrate by itself**, so when you run it from the IDE or the
CLI against `docker compose up -d database`, apply the migrations first — see [Migration](#migration).

### Restore Dependencies

To restore the dependencies, type the following command from the **repository root**:

```sh
./gradlew :planer-server:dependencies
```

### Run

To run the application from the CLI:

```sh
./gradlew :planer-server:run
```

The application will run by default on port **8080**. This can be changed via the `SERVER_PORT` environment variable or
by editing `application.conf` under `src/main/resources`. Stop it with `Ctrl+C`.

### Quick Test

The simplest check that the server is up and reachable is the health endpoint. It does not require authentication. Once
a database exists it will also report whether the database is reachable; **for now it only reports that the server is
running**:

```sh
curl http://localhost:8080/health
```

A healthy response looks like:

```json
{ "status": "healthy" }
```

Once the database check exists, an unreachable database will make the server return `503 Service Unavailable` with
`{ "status": "unhealthy" }`. The cause is written to the server log, not to the response.
See [Api → Health](../.docs/api/Api.md#health).

Once authentication exists, you can verify it is wired up with:

```
GET {{host}}/api/users/me
```

Without a token, this will return `401 Unauthorized`. **Today it returns `404`**, because that route does not exist yet.

## Sample Data

Sample data is provided for testing purposes of the application. To populate the database, use the following script from
the repository root:

```sh
./scripts/add-sample-data.sh
```

For details on what the sample data contains and how to reset it, check out
the [Sample Data documentation](../.docs/dev/Sample-Data.md).

## Build Executable

To build an executable JAR:

```sh
./gradlew :planer-server:buildFatJar
```

The resulting JAR is written to `planer-server/build/libs/` and can be run with:

```sh
java -jar planer-server/build/libs/planer-server-all.jar
```

## Project Structure

The code is grouped by **feature** first and by layer second. Everything lives under
`src/main/kotlin/app/planer/server/`:

| Folder                  | What lives there                                                                                                       |
|-------------------------|------------------------------------------------------------------------------------------------------------------------|
| `Application.kt`        | `module()`, the Ktor module named in `application.conf`. It only calls the plugins below                               |
| `common/plugins/`       | One file per Ktor plugin, each exposing a `configureX()` function (database, serialization, error handling, modules)   |
| `common/errors/`        | The exceptions that map to the API error codes, and the translation of SQL errors                                      |
| `auth/`                 | Who the caller is. A placeholder today (see below)                                                                     |
| `<feature>/`            | One folder per feature (`calendar`, `health`, and the coming `event`, `task`, `user`). See below                       |
| `src/main/resources/`   | `application.conf`, `logback.xml`, and the Flyway migrations in `db/migration/`                                        |
| `src/test/`             | Unit tests, mirroring the main packages                                                                                |

A feature folder holds a `<Feature>Module.kt`, which registers the feature's dependencies and mounts its routes (it is
loaded from `common/plugins/Modules.kt`), and up to three layers:

- `api/` — the routes, and the mapping from domain models to response DTOs
- `domain/` — the model, the service with the business rules, and the repository **interface**
- `data/` — the Exposed table and entity, and the repository **implementation**

The dependency points inwards: `api` and `data` know the domain, and the domain knows neither. **Calendar** is the first
feature built end to end, so copy its shape for a new one. A feature with no business logic, like `health`, needs only
its module and its routes.

The server's entry point is Ktor's `EngineMain` (the `application` plugin's main class); `application.conf` names the
module to load.

Authentication is a **placeholder**: the caller is whoever the `X-User-Id` header says it is (`auth/`), until JWT
authentication exists.

Planned, not yet created: `config/` (config loading), `security/` (JWT, password hashing), an
`application-test.conf` for integration tests, and the `src/integrationTest/` source set (test application, base
class, HTTP client helper, and tests grouped by collection).

## Architecture

PlaneR follows a **client–server** architecture with a shared **Kotlin Multiplatform** module (models, DTOs, validation,
API client — no UI). The server's role is well-defined even though its internal structure is still being decided:

**Responsibilities**

- **Authentication** — registration, login, token issuance and refresh, email verification, password reset
- **Authorization** — every request is scoped to the authenticated user
- **Persistence** — reading and writing domain models to PostgreSQL
- **Business logic** — recurrence expansion, task ↔ event conversion, cascade rules on deletion, ownership checks
- **HTTP surface** — the endpoints described in the [Api documentation](../.docs/api/Api.md)

**Request flow (high level)**

- A request arrives on a Ktor route, where it is validated
- The route delegates to a service that applies business rules
- The service calls into a repository that talks to the database
- The result travels back to the route and to the client, with errors handled centrally by the `StatusPages` plugin

The specific shape of the service and repository layers — whether there are explicit use cases, whether repositories
return domain models or persistence entities — is **intentionally undecided** and will be finalised once the codebase
exists. See [Architecture → Open Questions](../.docs/architecture/Architecture.md#open-questions) for the outstanding
decisions.

For more details, check out the full [Architecture documentation](../.docs/architecture/Architecture.md).

## Api

The full API documentation lives at [Api](../.docs/api/Api.md). It covers conventions, authentication, error format,
pagination, and links to each controller's own reference.

The controllers are:

- [Auth](../.docs/api/controllers/Api.Auth.md)
- [User](../.docs/api/controllers/Api.User.md)
- [Calendar](../.docs/api/controllers/Api.Calendar.md)
- [Event](../.docs/api/controllers/Api.Event.md)
- [Task](../.docs/api/controllers/Api.Task.md)
- [Summary](../.docs/api/controllers/Api.Summary.md)
- [Search](../.docs/api/controllers/Api.Search.md) *(sketch — not final)*

### Authentication

> **Not built yet.** No authentication or `/api` routes exist. This section describes the planned behaviour.

Endpoints that do not allow anonymous requests expect a **JWT** in the `Authorization` header, as a **Bearer Token**.
The server uses **HS256** for signing.

To obtain a token, call the login endpoint:

```
POST {{host}}/api/auth/login
```

With the body:

```json
{
    "email": "alex@example.com",
    "password": "Password123!"
}
```

The response includes an `accessToken` (valid for 15 minutes) and a `refreshToken` (valid for 60 days, rotated on every
use). Set the access token as the Bearer token in your HTTP client and every subsequent request will be authenticated;
when it expires, exchange the refresh token at `POST /api/auth/refresh`.

For more on the token model — signing, refresh, revocation, and expiry — see [Security](../.docs/security/Security.md).

## Database

The database chosen for this application infrastructure is **PostgreSQL**.

For more information about the database — naming conventions, schema, indexes, and value-object storage — please check
out the [Database documentation](../.docs/infrastructure/Database.md). Data access uses **Exposed** (DAO).

### Migration

**Flyway** is the database migration tool. Migration files are versioned SQL scripts under
`src/main/resources/db/migration/`. Applied migrations are never edited; mistakes are fixed with a new migration
(Flyway Community has no undo — see [Database → Rolling Back](../.docs/infrastructure/Database.md#rolling-back)).

The server does not apply migrations at startup. They are a separate step:

```sh
./gradlew :planer-server:flywayMigrate   # apply pending migrations
./gradlew :planer-server:flywayInfo      # show applied and pending migrations
./gradlew :planer-server:flywayClean     # drop everything (development only)
```

The database must be running (`docker compose up -d database`). The connection is read from the `DB_*` environment
variables and defaults to the development values. In containers, the `migrate` service of `docker-compose.yml` does
this automatically on `docker compose up -d` and can run other commands on demand
(`docker compose run --rm migrate info`, `... migrate clean`).

Migration files follow the naming convention `V{version}__{description}.sql`. The full workflow, including how to
rebuild the development database, is in [Database → Migrations](../.docs/infrastructure/Database.md#migrations).

## Testing

The server is tested at two levels — **Unit** and **Integration**. Both use **JUnit 5** with **Kotest assertions**.

End-to-end tests exist for the **clients** (desktop and Android) but not for the server in isolation.

The full test strategy is documented in [Testing](../.docs/testing/Testing.md). The server-specific details are
in [Testing — Server](../.docs/testing/Testing.Server.md).

### Unit Testing

Unit tests exercise a single class or function in isolation, with no database and no HTTP. They live under
`src/test/kotlin/app/planer/server/` and mirror the main package structure.

To run all unit tests:

```sh
./gradlew :planer-server:test
```

Today there is one test class, `HealthRoutesTest`, which uses Ktor's `testApplication` without a database.

The component under test is held in a field named **`uut`** (*Unit Under Test*).

### Integration Testing

> **Not built yet.** The `integrationTest` source set and task, Testcontainers, and the supporting classes below do not
> exist. This section describes the planned setup.

Integration tests exercise the server through its HTTP layer against a **real Postgres instance** running in a
**Testcontainers** container. They live under `src/integrationTest/kotlin/app/planer/server/integration/`, grouped by
test collection.

To run all integration tests:

```sh
./gradlew :planer-server:integrationTest
```

The supporting infrastructure lives in `src/integrationTest/kotlin/app/planer/server/core/`:

- **Test Application** — starts the Ktor app in-process for testing
- **BaseIntegrationTest** — every integration test extends this; provides the `client` and manages per-test state
- **AppHttpClient** — a thin wrapper around the Ktor test client with helpers for auth
- **TestAuthHandler** — injects a JWT for tests without hitting the login endpoint
- **ITestData** — an interface for seeding datasets

Each test collection (an abstract base class owning a container and a dataset) is backed by its own Postgres container,
shared across the test classes in it. The **Resetter** wipes the tables between test methods so tests remain isolated.

For the full picture — including how to write a collection, seed data, and assert with the client —
see [Testing — Server](../.docs/testing/Testing.Server.md).

## Live Reload

> **Not verified yet.** The commands below are the standard Ktor approach, but they have not been tried in this project.

Ktor has built-in support for auto-reload during development. When the server is started with development mode enabled,
it watches the compiled classes and reloads them when they change.

To run in development mode with continuous compilation:

```sh
./gradlew :planer-server:run --continuous
```

Any change to a Kotlin source file triggers recompilation, and Ktor picks up the new classes without a full restart.
Alternatively, the flag can be set explicitly:

```sh
./gradlew :planer-server:run -Dio.ktor.development=true
```

The server container runs the packaged JAR, so editing the source code on the host requires rebuilding the image
(`docker compose up -d --build server`). For the fastest loop, run the server from the IDE or CLI against the
containerized database (`docker compose up -d database`).

For more configuration options, check the `application.conf` file under `src/main/resources`, which is already included
in the application.
