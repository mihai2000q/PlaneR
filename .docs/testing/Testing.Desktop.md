# PlaneR Testing — Desktop

End-to-end tests for the desktop application drive the **real Compose for Desktop UI** against a **real running server** and a real database. They verify that the whole system works from the user's perspective.

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

### Running the Tests

To run the desktop UI tests:

```sh
./gradlew :planer-desktop:uiTest
```

The tests render the Compose UI off-screen, so no window appears. They connect the application to the running server and drive it through the UI.

The server must be running, and the sample data loaded (see [Sample Data](../dev/Sample-Data.md)), because the tests sign in as a seeded user. The application's server URL for tests is set in the test configuration and defaults to `http://localhost:8080`.

When a test fails and you need to see what the UI looked like, reproduce the steps by hand with `./gradlew :planer-desktop:run` against the same server, or capture a screenshot from the test with `captureToImage()` on the failing node.

## Conventions

E2E test classes are named after the feature or screen they exercise:

- `AuthScreenTest`
- `CalendarViewTest`
- `TaskListTest`

Most tests follow a **happy path** — error cases are covered by unit and integration tests, so the e2e layer focuses on the flow a real user would take.

Each test:

1. Launches the app
2. Navigates to the relevant screen
3. Performs user actions (`click`, `typeText`, `performScrollTo`, …)
4. Asserts on the visible UI (`assertIsDisplayed`, `assertTextEquals`, …)

Example:

```kotlin
class AuthScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `when valid credentials are submitted, then it navigates to the calendar`() {
        // given
        rule.setContent { PlaneRApp() }
        rule.onNodeWithText("Email").performTextInput("alex@example.com")
        rule.onNodeWithText("Password").performTextInput("Password123!")

        // when
        rule.onNodeWithText("Sign in").performClick()

        // then
        rule.onNodeWithText("Today").assertIsDisplayed()
    }
}
```

## Misc

### Custom Matchers

Custom matchers live in `planer-desktop/src/test/kotlin/matchers`. They are specific to the desktop module; the Android module keeps its own.

The example below is illustrative: `IsOverdue` is a custom semantics property that the app would have to define and set on the composable.

```kotlin
val IsOverdueKey = SemanticsPropertyKey<Boolean>("IsOverdue")

fun SemanticsNodeInteraction.assertIsOverdue(): SemanticsNodeInteraction {
    assert(SemanticsMatcher.expectValue(IsOverdueKey, true))
    return this
}
```

### Utils

Tests sign in as a seeded user by calling `ApiClient.login(...)` in test setup with the credentials from the sample data, then start the app already authenticated. Do not mint tokens by hand: that would require the server's signing key in the test code. (The in-process `X-Test-User-Id` shortcut described in [Testing — Server](Testing.Server.md#test-auth-handler) exists only in the server's own integration tests, not against a real running server.)

### Test Tags

When a node is hard to select by text or content description, use a **test tag** on the composable:

```kotlin
Modifier.testTag("event-card-${event.id}")
```

Then select it in the test:

```kotlin
rule.onNodeWithTag("event-card-22222222-...").assertIsDisplayed()
```

Tags are stable identifiers that survive copy changes and layout refactors. Keep tag names in a single `TestTags` object in the desktop module. The Android client defines its own; by convention the two use the same naming pattern (`event-card-<id>`) so that tests read alike, but nothing enforces it.