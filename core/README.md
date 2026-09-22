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
        - [Ui](#ui)
    - [Consuming Core](#consuming-core)
    - [Testing](#testing)
        - [Unit Testing](#unit-testing)
        - [Compose Ui Testing](#compose-ui-testing)

## Abstract

This is the shared library module of **PlaneR**, written in Kotlin using **Kotlin Multiplatform (KMP)**.

It is not a runnable application — it exists to be consumed by the [Server](../planer-server/README.md), [Desktop](../planer-desktop/README.md), and [Android](../planer-android/README.md) modules. Anything that can be shared between the server and the clients lives here: domain models, DTOs, validation rules, the networking client, and the shared **Compose Multiplatform** UI.

The goal is a single source of truth for what an `Event`, a `Task`, or a `RecurrenceRule` looks like, so that changes to the contract only need to be made once.

## Prerequisites

Before you can get started, there are some things you need to have installed on your system.

### Kotlin and JDK

The module is written in Kotlin and compiles for the JVM (and eventually for iOS), so you need the **JDK 21 or later**. If you don't have it already installed, please download it from here: `https://adoptium.net/`.

You also need the Kotlin compiler, but if you use the Gradle wrapper included in the project, it will handle the Kotlin version automatically.

No additional tooling is needed — Compose Multiplatform, Ktor client, and all other dependencies come through Gradle.

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
    │       └── ui/
    ├── commonTest/
    ├── androidMain/
    ├── androidUnitTest/
    ├── androidInstrumentedTest/
    ├── jvmMain/
    ├── jvmTest/
    ├── iosMain/          (future)
    └── iosTest/          (future)
```

### Domain

The shapes described in [Domain](../../.docs/domain/Domain.md) — `User`, `Calendar`, `Event`, `Task`, `RecurrenceRule`, `CalendarIcon` — as plain Kotlin data classes.

These are the **shared representation** used on both sides of the wire. They carry no framework annotations and no serialization concerns.

### Dto

Request and response bodies for the API described in [Api](../../.docs/api/Api.md). Kept separate from the domain models even though they are similar today — the API includes fields the domain does not (e.g., `occurrenceDate` on expanded recurring events), and the two will diverge over time.

DTOs are serialized with **kotlinx.serialization**.

### Validation

Rules like "title is 1–120 characters", "`endsAt` must be after `startsAt`", "color matches `^#[0-9A-Fa-f]{6}$`".

Validating on both client (for immediate UI feedback) and server (as the source of truth) using the same code eliminates drift between the two. Validation rules are pure Kotlin and have no dependencies on either side.

### Network

A thin HTTP client built on **Ktor Client** that both platforms use to talk to the server. It wraps the endpoints defined in [Api](../../.docs/api/Api.md) and returns typed responses.

There is one file per controller, mirroring the docs:

```
network/endpoint/
├── AuthEndpoints.kt
├── UserEndpoints.kt
├── CalendarEndpoints.kt
├── EventEndpoints.kt
└── TaskEndpoints.kt
```

The `ApiClient` itself handles token storage, header injection, and error mapping to a shared `ApiError` type.

### Ui

The screens, components, view models, and navigation, written with **Compose Multiplatform**. This is the largest part of the module.

```
ui/
├── theme/           # Colors, typography, spacing
├── component/       # Reusable composables (buttons, cards, pickers)
├── screen/          # Full screens, one per route
└── navigation/      # Nav graph and route definitions
```

Resources such as icons, fonts, and strings live in `commonMain/composeResources/` and are accessed through generated code — this is the standard Compose Multiplatform resource mechanism.

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

## Testing

### Unit Testing

Unit tests for the shared code live in `commonTest` and run on every target.
They follow the conventions described in [Testing](../../.docs/testing/Testing.md).

To run them for the JVM target only (fastest during development):

```sh
./gradlew :planer-core:jvmTest
```

To run them for all targets:

```sh
./gradlew :planer-core:allTests
```

Test fixtures such as fake repositories, in-memory storage, and token builders live under
`commonTest/kotlin/app/planer/core/testutil/` so they can be reused by any test in this module.

### Compose Ui Testing

Compose components are tested with **Compose UI Testing**, which works identically on
Compose for Desktop and Jetpack Compose.

To run UI tests on the desktop target:

```sh
./gradlew :planer-core:desktopTest
```

To run them on Android instrumented:

```sh
./gradlew :planer-core:connectedAndroidTest
```

Custom matchers live in `commonTest/kotlin/app/planer/core/testutil/matchers/`.