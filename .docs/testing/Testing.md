# PlaneR Testing

PlaneR is tested at three levels, and each level has its own document:

| Level | Scope | Document |
|---|---|---|
| **Unit** | Single classes in isolation | [Server](Testing.Server.md#unit-testing), [Desktop](Testing.Desktop.md), [Android](Testing.Android.md) |
| **Integration** | The server through its HTTP layer, against a real Postgres | [Server](Testing.Server.md#integration-testing) |
| **End-to-End** | Real clients, driven through their UI, against a real server | [Desktop](Testing.Desktop.md), [Android](Testing.Android.md) |

Tests are written in **Kotlin** and run through **JUnit 5** with **Kotest assertions** (`shouldBe`, `shouldNotBeNull`, …). Compose UI tests use **Compose UI Testing**, whose test rules are JUnit 4 (`@get:Rule`); the two client modules therefore also depend on JUnit 4 for their UI tests. The Compose UI Testing API is the same on Compose for Desktop and Jetpack Compose, but each client has its own test code — there is no shared UI to test in common.

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

Test methods follow the pattern `whenScenario_shouldExpectedOutcome`:

- `whenRegisterIsSuccessful_shouldCreateUserAndReturnToken`
- `whenEmailAlreadyExists_shouldThrowConflict`
- `whenPasswordIsTooShort_shouldFailValidation`

A shorter `whenScenario_shouldOutcome` form is accepted when the method under test is already clear from the class name.

### Structure

The component under test is held in a field. The name differs by test level:

| Level | Field name | Meaning |
|---|---|---|
| Unit | `uut` | Unit Under Test |
| Integration | `sut` | System Under Test |
| API integration | `client` | The Ktor test client |

Every method has three stages, marked by comments:

```kotlin
@Test
fun whenRegisterIsSuccessful_shouldCreateUserAndReturnToken() = runTest {
    // Arrange
    val command = RegisterUserCommand(...)

    // Act
    val result = uut.register(command)

    // Assert
    result.token.shouldNotBeNull()
}
```

The `Arrange / Act / Assert` pattern makes the intent of a test obvious at a glance and keeps tests small enough to read in one screen.

### Fixtures and Fakes

- **No mocking frameworks.** Hand-written fakes are simpler to read and debug. A `FakePasswordHasher`, `InMemoryUserRepository`, or `StubClock` is a five-line class.
- **Fixtures live in the test source set of the module that uses them** (`commonTest` in `planer-shared`, `test` in the server and clients). Test fixtures do not live in main source sets — production code should not depend on test code.
- **Test source sets are not visible to other modules.** If two modules genuinely need the same fixtures, expose them through a Gradle test-fixtures source set or a small dedicated testing module. Do not copy them, and do not assume `commonTest` of `planer-shared` is reachable from the clients.
- **Each test is independent.** A test must not rely on state left behind by another. Database state is reset between methods; in-memory state is recreated in the test class constructor.

## Where to Go Next

- [Server](Testing.Server.md) — unit and integration tests for the Ktor server
- [Desktop](Testing.Desktop.md) — end-to-end UI tests on Compose for Desktop
- [Android](Testing.Android.md) — end-to-end UI tests on Jetpack Compose for Android