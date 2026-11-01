# PlaneR Android

- [PlaneR Android](#planer-android)
    - [Abstract](#abstract)
    - [Prerequisites](#prerequisites)
        - [Kotlin and JDK](#kotlin-and-jdk)
        - [Android Studio](#android-studio)
        - [Android SDK](#android-sdk)
    - [Get Started](#get-started)
        - [Environment Variables](#environment-variables)
        - [Restore Dependencies](#restore-dependencies)
        - [Run](#run)
    - [Build](#build)
    - [Architecture](#architecture)
    - [Testing](#testing)
        - [Unit Testing](#unit-testing)
        - [Instrumented Testing](#instrumented-testing)
    - [Live Reload](#live-reload)

## Abstract

This is the Android client of **PlaneR**, written in Kotlin using **Jetpack Compose**.

This is a native client: it owns its entire UI — screens, components, navigation, and view models. It consumes the models, DTOs, validation rules, and networking client from [`planer-shared`](../planer-shared/README.md), which contains no UI. The desktop client has its own, separate UI.

iOS is planned as a future sibling module (`planer-ios`) that will also depend on `planer-shared`, with a native UI of its own. Adding it will not change this module.

## Prerequisites

Before you can get started, there are some things you need to have installed on your system.

### Kotlin and JDK

The application is written in Kotlin and runs on the JVM during development, so you need the **JDK 21 or later**. If you don't have it already installed, please download it from here: `https://adoptium.net/`.

You also need the Kotlin compiler, but if you use the Gradle wrapper included in the project, it will handle the Kotlin version automatically.

### Android Studio

The recommended IDE for developing the Android application is **Android Studio** (Ladybug or newer). It bundles the Android SDK, the emulator, and all the tooling needed to build and run the app. You can download it from here: `https://developer.android.com/studio`.

### Android SDK

The application targets **Android 14 (API 34)** and has a minimum supported version of **Android 8.0 (API 26)**. The required SDK components will be installed automatically by Android Studio on first launch, but you can also install them manually through the **SDK Manager**:

- **Android SDK Platform 34**
- **Android SDK Build-Tools 34.x**
- **Android Emulator**
- **Android SDK Platform-Tools**

## Get Started

To get started on contributing to this project, you can do so by following the next steps.

### Environment Variables

Duplicate the `.env.dev` file and rename it to `.env`. The Android application reads the server URL and other configuration values from this file at build time.

The expected variables are:

```env
SERVER_URL=http://10.0.2.2:8080
```

> **Note:** When running on the Android emulator, `10.0.2.2` is the special alias for the host machine's `localhost`. If you are running on a physical device, replace it with your machine's local IP address (e.g., `http://192.168.1.42:8080`).

### Restore Dependencies

To restore the dependencies, type the following command in the terminal from the **root** of the repository:

```sh
./gradlew :planer-android:build
```

Alternatively, if you only want to download the dependencies without building:

```sh
./gradlew :planer-android:dependencies
```

### Run

To run the application on a connected device or a running emulator, type the following command from the **root** of the repository:

```sh
./gradlew :planer-android:installDebug
```

If you are using **Android Studio**, you can also:

1. Open the project and wait for the Gradle sync to complete
2. Select the `planer-android` run configuration from the dropdown at the top
3. Choose your target device or emulator
4. Click the green **Run** button (or press `Shift + F10`)

The application will launch on the selected device and connect to the server specified in your `.env` file.

## Build

To build a debug APK for testing purposes:

```sh
./gradlew :planer-android:assembleDebug
```

The resulting APK will be located in `planer-android/build/outputs/apk/debug/`.

To build a release APK or an Android App Bundle (AAB) for distribution, you need to provide a signing key. Create a `keystore.properties` file in the root of the Android module with the following structure:

```properties
storeFile=path/to/your/keystore.jks
storePassword=yourStorePassword
keyAlias=yourKeyAlias
keyPassword=yourKeyPassword
```

Then build the signed release:

```bash
# For APK
./gradlew :planer-android:assembleRelease

# For AAB (recommended for Google Play)
./gradlew :planer-android:bundleRelease
```

The resulting AAB will be located in `planer-android/build/outputs/bundle/release/`.

## Architecture

The Android module is a complete native client. It uses the **shared** module for the contract with the server and provides everything the user sees.

The main responsibilities of this module are:

- **Application entry point** — the `MainActivity` that launches the Compose application
- **UI** — screens, components, navigation, and view models
- **Platform integrations** — Android-only features such as notifications, permissions, and background work
- **Token storage** — the Android Keystore–backed implementation of `TokenStorage`
- **Forced-update screen** — shown when `ApiClient` signals `UPGRADE_REQUIRED`
- **Android manifest** — declared permissions, activities, and services
- **Packaging** — Gradle configuration for building APK and AAB artifacts

Business rules do not live here: recurrence, routine layout, and ownership are the server's job, and validation rules come from [`planer-shared`](../planer-shared/README.md). Screens are **not** shared with the [desktop](../planer-desktop/README.md) application; a screen that exists on both is implemented on both.

For more details, check out the [Architecture documentation](../.docs/architecture/Architecture.md).

## Testing

### Unit Testing

Unit tests for the shared contract live in `planer-shared`. This module contains tests for the Android UI and view models.

To run the tests for the Android module specifically:

```sh
./gradlew :planer-android:test
```

To run all tests across the project:

```sh
./gradlew test
```

The framework is **Kotlin Test** with **JUnit 5**, alongside **Compose UI Testing** (which uses JUnit 4 rules) for rendering composables in isolation.

### Instrumented Testing

Instrumented tests run on a real Android device or emulator and verify platform-specific behaviour such as navigation, permissions, and interaction with the Android framework.

The instrumented tests reside in `src/androidTest`. To run them, you need a connected device or a running emulator, then:

```sh
./gradlew :planer-android:connectedAndroidTest
```

For the full end-to-end test strategy, including how to run the tests against a live server, see [Testing — Android](../.docs/testing/Testing.Android.md).

## Live Reload

To use live reload while developing the Android application, you can take advantage of **Jetpack Compose's Live Edit** feature, available in **Android Studio Ladybug** and newer.

Once you have the app running on a device or emulator:

1. Enable **Live Edit** by going to **Settings → Editor → Live Edit** and checking **Enable Live Edit**
2. Make a change to any composable in this module
3. The running app will update automatically without a full rebuild

For a more traditional continuous build experience, you can use the Gradle continuous build mode from the command line:

```sh
./gradlew :planer-android:installDebug --continuous
```

The above command will watch for file changes and reinstall the app automatically. For more configuration options, check the Gradle build script, which is already included in the application.