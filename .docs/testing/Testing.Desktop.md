# PlaneR Testing — Desktop

End-to-end tests for the desktop application drive the **real Compose for Desktop UI** against a **real running server** and a real database. They verify that the whole system works from the user's perspective.

Shared conventions (naming, Arrange/Act/Assert) are described in the [overview](Testing.md#conventions).

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

The tests run in a **headless JVM** by default. They launch the Compose application, connect it to the running server, and drive it through the UI.

To run them with a visible window (useful for debugging):

```sh
./gradlew :planer-desktop:uiTest -Dcompose.ui.test.headless=false
```

This is particularly helpful when a test fails and you need to see what the UI actually looked like at the moment of failure.

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
    fun whenValidCredentials_shouldNavigateToCalendar() {
        rule.setContent { PlaneRApp() }

        rule.onNodeWithText("Email").performTextInput("alex@example.com")
        rule.onNodeWithText("Password").performTextInput("Password123!")
        rule.onNodeWithText("Sign in").performClick()

        rule.onNodeWithText("Today").assertIsDisplayed()
    }
}
```

## Misc

### Custom Matchers

Custom matchers live in `planer-core/src/commonTest/kotlin/matchers`. They are shared with the Android tests, so anything added here is available on both platforms.

Example:

```kotlin
fun SemanticsNodeInteraction.assertIsOverdue(): SemanticsNodeInteraction {
    assert(SemanticsMatcher.expectValue(SemanticsProperties.IsOverdue, true))
    return this
}
```

### Utils

A helper to build a signed test token is provided in `planer-core/src/commonTest/kotlin/utils/TestTokens.kt`. Use it with `AppHttpClient.withToken(...)` when a test needs to act as a specific user without going through the login UI.

### Test Tags

When a node is hard to select by text or content description, use a **test tag** on the composable:

```kotlin
Modifier.testTag("event-card-${event.id}")
```

Then select it in the test:

```kotlin
rule.onNodeWithTag("event-card-22222222-...").assertIsDisplayed()
```

Tags are stable identifiers that survive copy changes and layout refactors, and they are shared with the Android tests.