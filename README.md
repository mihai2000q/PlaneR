# PlaneR

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Ktor](https://img.shields.io/badge/Ktor-087CFA?style=flat&logo=ktor&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![Compose for Desktop](https://img.shields.io/badge/Compose_for_Desktop-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat&logo=postgresql&logoColor=white)
![Exposed](https://img.shields.io/badge/Exposed-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)

![Version](https://img.shields.io/badge/version-0.1.0-1098ad)

> **Status:** Early development. The API, schema, and domain model are subject to change without notice until the first stable release.
>
> **What exists today:** a building skeleton of all four modules — the Gradle build, a shared module with a health DTO and two validation rules, a Ktor server that serves `GET /health`, and a desktop and an Android app that each show a placeholder screen. Nothing else is implemented yet; the documentation describes the design the project is being built towards, and the module READMEs mark which parts exist.

- [PlaneR](#planer)
  - [Abstract](#abstract)
  - [Prerequisites](#prerequisites)
  - [Get Started](#get-started)
    - [Shared](#shared)
    - [Server](#server)
    - [Desktop](#desktop)
    - [Android](#android)
  - [Documentation](#documentation)
  - [License](#license)

## Abstract

**PlaneR** is a time planning application written in Kotlin. It is designed to help users organize their schedules, create events, set reminders, and manage their time more effectively. The clients are **separate applications**: each one has its own UI written against its platform's toolkit, while the non-visual code is shared through a **Kotlin Multiplatform (KMP)** library. Android is a native Android app; the desktop app runs on the JVM, and **Compose for Desktop** draws its own UI instead of using the operating system's widgets, so it is cross-platform rather than native.

The project is divided into four modules:

- **Shared** — The shared KMP library containing domain models, DTOs, validation logic, and the networking client. It contains no UI code.
- **Server** — A RESTful API built with **Ktor**, responsible for data persistence, authentication, and business logic.
- **Desktop** — A desktop application built with **Compose for Desktop**, targeting Windows, macOS, and Linux.
- **Android** — The mobile application built with **Jetpack Compose**, targeting Android. iOS support is planned as a future sibling module (native, with its own UI).

Desktop and Android do **not** share screens or view models. They share the toolkit family (Compose) by choice, not by architecture — each client owns its UI, and either could move to a different toolkit without touching the other or **Shared**.

## Prerequisites

To run PlaneR locally, you need:

- **JDK 21 or later** — `https://adoptium.net/`
- **Docker** — `https://www.docker.com/products/docker-desktop/` (for the database and server containers; not needed until the database exists)
- **Android Studio** (only if developing the Android application) — `https://developer.android.com/studio`, with **Android SDK Platform 37** installed
- **IntelliJ IDEA** (recommended for the shared and server code) — `https://www.jetbrains.com/idea/`

You do **not** need to install Gradle: the repository contains the Gradle wrapper (`gradlew` / `gradlew.bat`). Node.js, Python, or any other runtime is not required.

## Get Started

The whole project is one **Gradle** build with four modules (`planer-shared`, `planer-server`, `planer-desktop`, `planer-android`). Run every command from the repository root.

**What works today:**

```sh
./gradlew build                         # build and test everything (Android needs the SDK, see Prerequisites)
./gradlew :planer-server:run            # start the server on http://localhost:8080  (try: curl localhost:8080/health)
./gradlew :planer-desktop:run           # open the desktop app
./gradlew :planer-android:installDebug  # install the Android app on a connected device or emulator
```

On Windows PowerShell or `cmd`, use `gradlew.bat` instead of `./gradlew`.

To get started on the application, you eventually need:

- the **server** up and running with its dependencies (a database, planned)
- and at least one of the platform applications (**Desktop** or **Android**)

**Planned, not built yet:** the `startup.sh` script (launch the server and database containers in one step) and the `run-container.sh` script (run everything in containers without installing a JDK) exist but are **empty**, and there is no `docker-compose.yml` yet. Until they are written, follow the setup of each module below (only one of Desktop or Android is needed, depending on where you prefer to run the app).

### Shared

This is the shared library module. It contains the domain models, DTOs, validation rules, and the networking client. It has no UI.

Shared is not a runnable application on its own — it exists to be consumed by the Desktop and Android clients. The Server depends on it only for the models, DTOs, and validation rules.

For a detailed setup, check out the [documentation](planer-shared/README.md).

### Server

This is the HTTP Server (or backend) of the **PlaneR** application, which is written completely in Kotlin using the **Ktor** framework.

Once running, the server exposes a health endpoint at `GET /health`. It returns `200 OK` when the server is healthy and, once the database exists, `503 Service Unavailable` when the database connection is not. (Today there is no database, so it only reports that the server is up.) This is used by the deployment platform for uptime monitoring and does not require authentication. See [Api](.docs/api/Api.md#health) for the response format.

For a detailed setup on the server, check out the [documentation](planer-server/README.md).

### Desktop

This is the desktop application, which is written in Kotlin using **Compose for Desktop**. It targets Windows, macOS, and Linux via the JVM.

For a detailed setup on the desktop platform, check out the [documentation](planer-desktop/README.md).

### Android

This is the Android application, which is written in Kotlin using **Jetpack Compose**. It is the current mobile target. An iOS application is planned as a future sibling module that will reuse the same **Shared** code.

For a detailed setup on the Android platform, check out the [documentation](planer-android/README.md).

## Documentation

The full documentation lives in [`.docs/`](.docs/). Each document is a focused reference for one area of the system.

- **[Features](.docs/features/Features.md)** — What the application does, and what is planned or deferred.
- **[Architecture](.docs/architecture/Architecture.md)** — How the modules fit together, the request flow, and cross-cutting concerns.
- **[Error Handling](.docs/architecture/Error-Handling.md)** — How errors flow from server to client.
- **[Security](.docs/security/Security.md)** — Access control, passwords, access/refresh/one-time tokens, transport security, and rate limiting.
- **[Database](.docs/infrastructure/Database.md)** — Naming conventions, schema, indexes, Flyway migrations, and data access.
- **[Deployment](.docs/infrastructure/Deployment.md)** — Environments, CI, releases, TLS, backups, and secrets.
- **[Domain](.docs/domain/Domain.md)** — Domain models, timezones, task/event conversion, recurrence, and preparation steps.
- **[Api](.docs/api/Api.md)** — Conventions, authentication, error format, and pagination.
- **[Testing](.docs/testing/Testing.md)** — Unit, integration, and end-to-end test strategy.
- **[Git](.docs/dev/Git.md)** — Issue labels, branch naming, and commit conventions.
- **[Glossary](.docs/dev/Glossary.md)** — Terms used throughout the project, with their precise meanings.
- **[Sample Data](.docs/dev/Sample-Data.md)** — Seeding the database for development.

## License

The project is licensed under the [Apache](https://www.apache.org/licenses/LICENSE-2.0) license.

---

**PlaneR** © 2026