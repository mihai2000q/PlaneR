# PlaneR Testing

PlaneR is tested at three levels, and each level has its own document:

| Level | Scope | Document |
|---|---|---|
| **Unit** | Single classes in isolation | [Server](Testing.Server.md#unit-testing), [Desktop](Testing.Desktop.md), [Android](Testing.Android.md) |
| **Integration** | The server through its HTTP layer, against a real Postgres | [Server](Testing.Server.md#integration-testing) |
| **End-to-End** | Real clients, driven through their UI, against a real server | [Desktop](Testing.Desktop.md), [Android](Testing.Android.md) |

Tests are written in **Kotlin** and run through **JUnit 5** with **Kotest assertions** (`shouldBe`, `shouldNotBeNull`, …). Compose UI tests use **Compose UI Testing**, but the two clients use it differently. **Desktop** component tests use the `runComposeUiTest` API (`androidx.compose.ui.test.v2`), which needs no JUnit 4 rule and runs on the JUnit 5 platform. **Android** UI tests are *instrumented* tests run by Android's JUnit 4 runner, so they use the `createComposeRule` rule (`androidx.compose.ui.test.junit4.v2`, `@get:Rule`). Each client has its own test code — there is no shared UI to test in common.

- [Conventions](#conventions)
  - [Naming](#naming)
  - [Structure](#structure)
  - [Fixtures and Fakes](#fixtures-and-fakes)

## Conventions

These conventions apply to every test in the project, regardless of module or level.

### Naming

The test class is named after the class or feature under test, followed by `Test`:

- `RegisterUserServiceTest` — unit test for `RegisterUserService`
- `AuthScreenTest` — UI test for the auth screen
- `EventRoutesTest` — integration test for the event routes

Test methods are named with a **backtick sentence** that reads as a **when … then …** statement:

- ``fun `when register is successful, then it creates the user and returns a token`()``
- ``fun `when the email already exists, then register fails with a conflict`()``
- ``fun `when the password is too short, then validation fails`()``

Start with `given …` only when the starting state is not obvious from the rest of the name (``given an unverified account, when changing the email, then it is forbidden``). Keep names to one sentence; if it needs two, the test is probably doing too much.

**Where backticks with spaces are not allowed.** The JVM accepts them, so they work in the server, the desktop app, and the JVM and Android *host* tests of `planer-shared`. They do **not** work in **Android instrumented tests** (`planer-android/src/androidTest`) while `minSdk` is below 30, because the dex format on older Android versions rejects spaces in method names. There, write the same sentence as camelCase clauses joined by an underscore:

```kotlin
fun whenValidCredentials_thenNavigatesToCalendar()
```

The same applies to any future Kotlin/JS or Kotlin/Native target. Everywhere else, use the backtick form.

### Structure

The component under test is held in a field. The name differs by test level:

| Level | Field name | Meaning |
|---|---|---|
| Unit | `uut` | Unit Under Test |
| Integration | `sut` | System Under Test |
| API integration | `client` | The Ktor test client |

Every method has three stages, marked by comments in the **Given / When / Then** style:

```kotlin
@Test
fun `when register is successful, then it creates the user and returns a token`() = runTest {
    // given
    val command = RegisterUserCommand(...)

    // when
    val result = uut.register(command)

    // then
    result.token.shouldNotBeNull()
}
```

- **given** — the state and inputs the scenario starts from
- **when** — the single action under test
- **then** — what must now be true

(This is the same three-part structure as Arrange / Act / Assert, with BDD labels.) It makes the intent of a test obvious at a glance and keeps tests small enough to read in one screen. A test has exactly one `when`; if it needs a second, split it into two tests. A trivial test may fold `given` into its inputs, but the three comments stay so every test looks the same.

### Fixtures and Fakes

- **No mocking frameworks.** Hand-written fakes are simpler to read and debug. A `FakePasswordHasher`, `InMemoryUserRepository`, or `StubClock` is a five-line class.
- **Fixtures live in the test source set of the module that uses them** (`commonTest` in `planer-shared`, `test` in the server and clients). Test fixtures do not live in main source sets — production code should not depend on test code.
- **Test source sets are not visible to other modules.** If two modules genuinely need the same fixtures, expose them through a Gradle test-fixtures source set or a small dedicated testing module. Do not copy them, and do not assume `commonTest` of `planer-shared` is reachable from the clients.
- **Each test is independent.** A test must not rely on state left behind by another. Database state is reset between methods; in-memory state is recreated in the test class constructor.

## Where to Go Next

- [Server](Testing.Server.md) — unit and integration tests for the Ktor server
- [Desktop](Testing.Desktop.md) — end-to-end UI tests on Compose for Desktop
- [Android](Testing.Android.md) — end-to-end UI tests on Jetpack Compose for Android