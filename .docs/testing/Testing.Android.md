# PlaneR Testing — Android

End-to-end tests for the Android application drive the **real Jetpack Compose UI** on an **Android emulator or device** against a **real running server** and a real database.

Shared conventions (naming, Given/When/Then) are described in the [overview](Testing.md#conventions).

- [Get Started](#get-started)
- [Conventions](#conventions)
- [Misc](#misc)

## Get Started

### Docker Environment

The e2e tests expect a working server and database. Follow the [server Get Started](../../planer-server/README.md#docker-containers) to:

- create a `.env` file
- start the containers with `docker compose up -d`
- apply the migrations

Alternatively, the `startup.sh` script at the repository root does all of this in one step.

### Device or Emulator

The tests run on a connected Android device or a running emulator. Android Studio manages both; from the **Device Manager**, launch any emulator with API 26 or higher.

To verify the device is visible to Gradle:

```sh
./gradlew :planer-android:connectedCheck --dry-run
```

If the task lists a target device, you're ready.

### Running the Tests

To run the Android instrumented tests:

```sh
./gradlew :planer-android:connectedAndroidTest
```

The tests are installed and executed on the device, then the results are reported back through Gradle.

## Conventions

E2E test classes are named after the feature or screen they exercise, mirroring the desktop conventions:

- `AuthScreenTest`
- `CalendarViewTest`
- `TaskListTest`

Most tests follow a **happy path** — error cases are covered by unit and integration tests.

Each test:

1. Launches the app
2. Navigates to the relevant screen
3. Performs user actions (`click`, `typeText`, `performScrollTo`, …)
4. Asserts on the visible UI (`assertIsDisplayed`, `assertTextEquals`, …)

Example:

```kotlin
class AuthScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun whenValidCredentialsAreSubmitted_thenNavigatesToCalendar() {
        // given
        rule.onNodeWithText("Email").performTextInput("alex@example.com")
        rule.onNodeWithText("Password").performTextInput("Password123!")

        // when
        rule.onNodeWithText("Sign in").performClick()

        // then
        rule.onNodeWithText("Today").assertIsDisplayed()
    }
}
```

There are two differences from the desktop test:

- **The rule.** `createAndroidComposeRule<MainActivity>()` launches the app from `MainActivity`, while desktop uses `createComposeRule()` with `setContent`.
- **The method name.** Instrumented tests cannot use backtick names with spaces while `minSdk` is below 30, so the same `when … then …` sentence is written as camelCase clauses joined by an underscore. See [Testing → Naming](Testing.md#naming).

## Misc

### Custom Matchers

Custom matchers live in `planer-android/src/androidTest/kotlin/matchers`. They are specific to the Android module; the desktop module keeps its own.

### Utils

Tests sign in as a seeded user by calling `ApiClient.login(...)` in test setup with the credentials from the sample data (see [Sample Data](../dev/Sample-Data.md)), then start the app already authenticated. Do not mint tokens by hand: that would require the server's signing key in the test code. The application under test reaches the host machine's server at `http://10.0.2.2:8080`, which needs cleartext HTTP allowed in the debug build's network security config.

### Test Tags

When a node is hard to select by text or content description, use a **test tag** on the composable:

```kotlin
Modifier.testTag("event-card-${event.id}")
```

Then select it in the test:

```kotlin
rule.onNodeWithTag("event-card-22222222-...").assertIsDisplayed()
```

Tags are stable identifiers that survive copy changes and layout refactors. Keep tag names in a single `TestTags` object in the Android module. The desktop client defines its own; by convention the two use the same naming pattern (`event-card-<id>`), but nothing enforces it.

### Emulator Slowdowns

Instrumented tests on an emulator are noticeably slower than desktop tests. To keep them fast:

- Prefer targeting a single device at a time
- Run `connectedAndroidTest` for a specific module and class rather than the entire suite during development
- Use `adb shell am instrument -w` directly when iterating on a single test

Emulators with API 30+ and hardware acceleration enabled run significantly faster than older or software-rendered ones.