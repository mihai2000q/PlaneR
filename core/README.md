# PlaneR Core

- [PlaneR Core](#planer-core)
    - [Abstract](#abstract)
    - [Prerequisites](#prerequisites)
        - [Kotlin and JDK](#kotlin-and-jdk)
    - [Get Started](#get-started)
        - [Restore Dependencies](#restore-dependencies)
        - [Build](#build)
        - [Typecheck](#typecheck)
    - [Structure](#structure)
        - [Domain](#domain)
        - [Dto](#dto)
        - [Validation](#validation)
        - [Network](#network)
        - [Token Storage](#token-storage)
    - [What Core Does Not Contain](#what-core-does-not-contain)
    - [Consuming Core](#consuming-core)
    - [Testing](#testing)

## Abstract

This is the shared library module of **PlaneR**, written in Kotlin using **Kotlin Multiplatform (KMP)**.

It is not a runnable application — it exists to be consumed by the [Server](../planer-server/README.md), [Desktop](../planer-desktop/README.md), and [Android](../planer-android/README.md) modules. It holds the *contract* between the server and the clients: domain models, DTOs, validation rules, and the networking client.

The goal is a single source of truth for what an `Event`, a `Task`, or a `RecurrenceRule` looks like, so that changes to the contract only need to be made once.

Core contains **no UI**. Desktop and Android are native applications that each own their screens and view models.

## Prerequisites

Before you can get started, there are some things you need to have installed on your system.

### Kotlin and JDK

The module is written in Kotlin and compiles for the JVM and Android (and eventually for iOS), so you need the **JDK 21 or later**. If you don't have it already installed, please download it from here: `https://adoptium.net/`.

You also need the Kotlin compiler, but if you use the Gradle wrapper included in the project, it will handle the Kotlin version automatically.

No additional tooling is needed — Ktor client, kotlinx.serialization, and all other dependencies come through Gradle.

## Get Started

To get started on contributing to this module, you can do so by following the next steps.

### Restore Dependencies

To restore the dependencies, type the following command in the terminal from the **root** of the repository:

```sh
./gradlew :planer-core:build
```

Alternatively, if you only want to download the dependencies without building:

```sh
./gradlew :planer-core:dependencies
```

### Build

To compile the module for all its targets (JVM, Android, and later iOS):

```sh
./gradlew :planer-core:assemble
```

To build only for the JVM target:

```sh
./gradlew :planer-core:jvmJar
```

### Typecheck

Because **Core** is a library, it cannot be run standalone. To verify that the code compiles without running any tests, use the build task above, or run:

```sh
./gradlew :planer-core:compileKotlinJvm
```

## Structure

The module follows the standard KMP layout, with `commonMain` holding the bulk of the code and platform-specific source sets only for `expect`/`actual` implementations.

```
planer-core/
├── README.md
├── build.gradle.kts
└── src/
    ├── commonMain/
    │   └── kotlin/app/planer/core/
    │       ├── domain/
    │       ├── dto/
    │       ├── validation/
    │       ├── network/
    │       └── auth/
    ├── commonTest/
    ├── androidMain/
    ├── jvmMain/
    ├── jvmTest/
    ├── iosMain/          (future)
    └── iosTest/          (future)
```

### Domain

The shapes described in [Domain](../.docs/domain/Domain.md) — `User`, `Calendar`, `Event`, `Task`, `RecurrenceRule`, `PreparationStep`, `CalendarIcon` — as plain Kotlin data classes.

These are the in-memory representation used by the server's business logic and by the clients. They carry no framework annotations and no serialization concerns.

### Dto

Request and response bodies for the API described in [Api](../.docs/api/Api.md). Kept separate from the domain models even though they are similar today — the API includes fields the domain does not (e.g., `occurrenceDate` on expanded recurring events), and the two will diverge over time.

DTOs are serialized with **kotlinx.serialization**.

### Validation

Rules like "title is 1–120 characters", "`endsAt` must be after `startsAt`", "color matches `^#[0-9A-Fa-f]{6}$`".

Validating on both client (for immediate UI feedback) and server (as the source of truth) using the same code eliminates drift between the two. Validation rules are pure Kotlin and have no dependencies on either side.

### Network

A thin HTTP client built on **Ktor Client** that both clients use to talk to the server. It wraps the endpoints defined in [Api](../.docs/api/Api.md) and returns typed responses.

There is one file per controller, mirroring the docs:

```
network/endpoint/
├── AuthEndpoints.kt
├── UserEndpoints.kt
├── CalendarEndpoints.kt
├── EventEndpoints.kt
└── TaskEndpoints.kt
```

The `ApiClient` itself is responsible for:

- attaching `X-Client-Platform` and `X-Client-Version` to every request (the app passes both in at construction)
- injecting the access token and refreshing it once on `401 UNAUTHORIZED`
- mapping non-2xx responses to the shared `ApiError` type — see [Error Handling](../.docs/architecture/Error-Handling.md)
- exposing `UPGRADE_REQUIRED` as a global signal that the app turns into its forced-update screen

### Token Storage

Core defines a `TokenStorage` interface; it does not implement it. Where a refresh token can be kept safely is platform-specific, so Android provides a Keystore-backed implementation and Desktop provides an OS-keychain-backed one. `ApiClient` receives the implementation at construction.

## What Core Does Not Contain

- **No UI.** No composables, no screens, no navigation, no theme.
- **No view models or state holders.** Each client writes its own.
- **No server code.** Persistence, authentication, and business rules live in the server.

If a piece of code needs a UI toolkit type, it belongs in `planer-desktop` or `planer-android`.

## Consuming Core

The other modules depend on **Core** in their `build.gradle.kts`:

**Server**

```kotlin
dependencies {
    implementation(project(":planer-core"))
}
```

**Desktop**

```kotlin
dependencies {
    implementation(project(":planer-core"))
}
```

**Android**

```kotlin
dependencies {
    implementation(project(":planer-core"))
}
```

Because planer-core is a KMP module, it automatically publishes the correct target artifact to each consumer — the JVM variant to Server and Desktop, and the Android variant to the Android client.

The Server uses only the models, DTOs, and validation rules. It does not call the networking client, although the client library ends up on its classpath.

## Testing

Unit tests live in `commonTest` and run on every target. They follow the conventions described in [Testing](../.docs/testing/Testing.md).

To run them for the JVM target only (fastest during development):

```sh
./gradlew :planer-core:jvmTest
```

To run them for all targets:

```sh
./gradlew :planer-core:allTests
```

Test fixtures such as fake token storage, in-memory fakes, and DTO builders live under
`commonTest/kotlin/app/planer/core/testutil/` and are reused by tests **within this module**.

Source sets named `commonTest` are not visible to other modules. If the server or a client needs the same fixtures, expose them as a Gradle **test fixtures** source set or a small dedicated testing module rather than copying them. Until that need appears, each module keeps its own fixtures.
