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

This is the desktop client of **PlaneR**, written in Kotlin using **Compose for Desktop** (part of the **Compose Multiplatform** umbrella). It targets Windows, macOS, and Linux via the JVM.

The module consumes shared business logic and UI code from [`planer-core`](../planer-core/README.md), so most of the screens and components live there. This module is primarily responsible for the desktop entry point, platform-specific configuration, and packaging.

## Prerequisites

Before you can get started, there are some things you need to have installed on your system.

### Kotlin and JDK

The application is written in Kotlin and runs on the JVM, so you need the **JDK 21 or later**. If you don't have it already installed, please download it from here: `https://adoptium.net/`.

You also need the Kotlin compiler, but if you use the Gradle wrapper included in the project, it will handle the Kotlin version automatically.

### Compose for Desktop

No separate installation is needed — the **Compose for Desktop** dependency is pulled in through Gradle. However, to take advantage of hot reload and preview features, it is recommended to use **IntelliJ IDEA** (Community or Ultimate) with the **Compose Multiplatform** plugin installed.

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

The desktop module is intentionally thin: it acts as an entry point that wires up the shared **core** module with desktop-specific configuration.

The main responsibilities of this module are:

- **Application entry point** — the `main` function that launches the Compose application
- **Window configuration** — size, title, icon, and platform-specific settings
- **Platform integrations** — desktop-only features such as tray icons, native menus, and file dialogs
- **Packaging** — Gradle configuration for building native installers

All UI code, screens, view models, and business logic live in [`planer-core`](../planer-core/README.md) so they can be shared with the [Android](../planer-android/README.md) client. If you need to add a new screen or component, it should almost always go into `planer-core`, not here.

For more details, check out the [Architecture documentation](../.docs/architecture/Architecture.md).

## Testing

### Unit Testing

Unit tests for shared components live in `planer-core`. This module only contains tests for desktop-specific behaviour.

To run the tests for the desktop module specifically:

```sh
./gradlew :planer-desktop:test
```

To run all tests across the project:

```sh
./gradlew test
```

The framework is **Kotlin Test** with **JUnit 5**, alongside **Compose UI Testing** for rendering shared components in isolation.

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
3. Any changes made to the shared UI code in `planer-core` will be reflected in the running window without a full restart

Alternatively, from the command line, you can use the Gradle continuous build mode:

```sh
./gradlew :planer-desktop:run --continuous
```

The above command will watch for file changes and rebuild the application automatically. For more configuration options, check the Gradle build script, which is already included in the application.