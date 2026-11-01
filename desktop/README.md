# PlaneR Desktop

- [PlaneR Desktop](#planer-desktop)
    - [Abstract](#abstract)
    - [Prerequisites](#prerequisites)
        - [Kotlin and JDK](#kotlin-and-jdk)
        - [Compose for Desktop](#compose-for-desktop)
    - [Get Started](#get-started)
        - [Environment Variables](#environment-variables)
        - [Restore Dependencies](#restore-dependencies)
        - [Run](#run)
    - [Build](#build)
    - [Architecture](#architecture)
    - [Testing](#testing)
        - [Unit Testing](#unit-testing)
        - [End-to-End Testing](#end-to-end-testing)
    - [Live Reload](#live-reload)

## Abstract

This is the desktop client of **PlaneR**, written in Kotlin using **Compose for Desktop**. It targets Windows, macOS, and Linux via the JVM.

This is a native client: it owns its entire UI — screens, components, navigation, and view models. It consumes the models, DTOs, validation rules, and networking client from [`planer-shared`](../planer-shared/README.md), which contains no UI. The Android client has its own, separate UI.

## Prerequisites

Before you can get started, there are some things you need to have installed on your system.

### Kotlin and JDK

The application is written in Kotlin and runs on the JVM, so you need the **JDK 21 or later**. If you don't have it already installed, please download it from here: `https://adoptium.net/`.

You also need the Kotlin compiler, but if you use the Gradle wrapper included in the project, it will handle the Kotlin version automatically.

### Compose for Desktop

No separate installation is needed — the **Compose for Desktop** dependency is pulled in through Gradle. However, to take advantage of hot reload and preview features, it is recommended to use **IntelliJ IDEA** (Community or Ultimate) with the **Compose Multiplatform** IDE plugin installed (the plugin provides tooling for Compose for Desktop; the app itself does not use shared Compose Multiplatform UI code).

## Get Started

To get started on contributing to this project, you can do so by following the next steps.

### Environment Variables

Duplicate the `.env.dev` file and rename it to `.env`. The desktop application reads the server URL and other configuration values from this file at build time.

The expected variables are:

```env
SERVER_URL=http://localhost:8080
```

### Restore Dependencies

To restore the dependencies, type the following command in the terminal from the **root** of the repository:

```sh
./gradlew :planer-desktop:build
```

Alternatively, if you only want to download the dependencies without building:

```sh
./gradlew :planer-desktop:dependencies
```

### Run

To run the application in development, type the following command from the **root** of the repository:

```sh
./gradlew :planer-desktop:run
```

The desktop application will launch in a native window, connecting to the server specified in your `.env` file.

If you are using **IntelliJ IDEA**, you can also run the application directly from the IDE by opening the `Main.kt` file and clicking the green run icon next to the `main` function.

## Build

To build a distributable version of the application, you need to choose the target system. The `packageDistributionForCurrentOS` task will detect your current OS automatically:

```sh
./gradlew :planer-desktop:packageDistributionForCurrentOS
```

If you want to build installers for a specific platform, use the following tasks:

```bash
# For Windows (MSI)
./gradlew :planer-desktop:packageMsi

# For macOS (DMG)
./gradlew :planer-desktop:packageDmg

# For Linux (DEB)
./gradlew :planer-desktop:packageDeb
```

The resulting installers will be located in `planer-desktop/build/compose/binaries/`.

To build an executable JAR that can be run on any platform with a JVM installed:

```sh
./gradlew :planer-desktop:packageUberJarForCurrentOS
```

## Architecture

The desktop module is a complete native client. It uses the **shared** module for the contract with the server and provides everything the user sees.

The main responsibilities of this module are:

- **Application entry point** — the `main` function that launches the Compose application
- **UI** — screens, components, navigation, and view models
- **Window configuration** — size, title, icon, and platform-specific settings
- **Platform integrations** — desktop-only features such as tray icons, native menus, and file dialogs
- **Token storage** — the OS-keychain-backed implementation of `TokenStorage`
- **Forced-update screen** — shown when `ApiClient` signals `UPGRADE_REQUIRED`
- **Packaging** — Gradle configuration for building native installers

Business rules do not live here: recurrence, routine layout, and ownership are the server's job, and validation rules come from [`planer-shared`](../planer-shared/README.md). Screens are **not** shared with the [Android](../planer-android/README.md) client; a screen that exists on both is implemented on both.

For more details, check out the [Architecture documentation](../.docs/architecture/Architecture.md).

## Testing

### Unit Testing

Unit tests for the shared contract live in `planer-shared`. This module contains tests for the desktop UI and view models.

To run the tests for the desktop module specifically:

```sh
./gradlew :planer-desktop:test
```

To run all tests across the project:

```sh
./gradlew test
```

The framework is **Kotlin Test** with **JUnit 5**, alongside **Compose UI Testing** (which uses JUnit 4 rules) for rendering composables in isolation.

### End-to-End Testing

End-to-end tests drive the real desktop UI against a running server, verifying that the whole system works from the user's perspective.

```sh
./gradlew :planer-desktop:uiTest
```

For the full strategy — including how to run the tests headlessly, custom matchers, and test tags — see [Testing — Desktop](../.docs/testing/Testing.Desktop.md).

## Live Reload

To use live reload while developing the desktop application, you need to run it with the **Compose Hot Reload** feature enabled. This is available through the **Compose Multiplatform** IntelliJ plugin.

Once the plugin is installed:

1. Open the project in **IntelliJ IDEA**
2. Run the `main` function in `Main.kt` with the **Run with Hot Reload** option
3. Any changes made to the UI code in this module will be reflected in the running window without a full restart

Alternatively, from the command line, you can use the Gradle continuous build mode:

```sh
./gradlew :planer-desktop:run --continuous
```

The above command will watch for file changes and rebuild the application automatically. For more configuration options, check the Gradle build script, which is already included in the application.