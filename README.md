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
- **Docker** — `https://www.docker.com/products/docker-desktop/` (for the database and server containers)
- **Android Studio** (only if developing the Android application) — `https://developer.android.com/studio`
- **IntelliJ IDEA** (recommended for the shared and server code) — `https://www.jetbrains.com/idea/`

Node.js, Python, or any other runtime is not required.

## Get Started

To get started on the application, you need:

- the **server** up and running with its dependencies
- and at least one of the platform applications (**Desktop** or **Android**)

If you have **JDK 21+** and **Docker** installed, you can take the shortest route. Fire the following shell script: `startup.sh` to launch the server and database containers.

Otherwise, follow the setup of each module below (only one of Desktop or Android is needed, depending on where you prefer to run the app).

Additionally, if you prefer to run everything in containers and not install JDK or Gradle, you can run the `run-container.sh` script to run all the Docker containers and apply the database migrations.

### Shared

This is the shared library module. It contains the domain models, DTOs, validation rules, and the networking client. It has no UI.

Shared is not a runnable application on its own — it exists to be consumed by the Desktop and Android clients. The Server depends on it only for the models, DTOs, and validation rules.

For a detailed setup, check out the [documentation](planer-shared/README.md).

### Server

This is the HTTP Server (or backend) of the **PlaneR** application, which is written completely in Kotlin using the **Ktor** framework.

Once running, the server exposes a health endpoint at `GET /health` that returns `200 OK` when both the server and its database connection are healthy, and `503 Service Unavailable` otherwise. This is used by the deployment platform for uptime monitoring and does not require authentication. See [Api](.docs/api/Api.md#health) for the response format.

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
- **[Security](.docs/security/Security.md)** — Access control, password storage, JWT, transport security, and input validation.
- **[Database](.docs/infrastructure/Database.md)** — Naming conventions, schema, and Flyway migration workflow.
- **[Deployment](.docs/infrastructure/Deployment.md)** — Environments, CI/CD pipelines, TLS, backups, and secrets.
- **[Domain](.docs/domain/Domain.md)** — Domain models, value objects, enums, task/event conversion, and recurrence.
- **[Api](.docs/api/Api.md)** — Conventions, authentication, error format, and pagination.
- **[Testing](.docs/testing/Testing.md)** — Unit, integration, and end-to-end test strategy.
- **[Git](.docs/dev/Git.md)** — Issue labels, branch naming, and commit conventions.
- **[Glossary](.docs/dev/Glossary.md)** — Terms used throughout the project, with their precise meanings.
- **[Sample Data](.docs/dev/Sample-Data.md)** — Seeding the database for development.

## License

The project is licensed under the [Apache](https://www.apache.org/licenses/LICENSE-2.0) license.

---

**PlaneR** © 2026