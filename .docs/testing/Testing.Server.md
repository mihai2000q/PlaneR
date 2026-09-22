# PlaneR Testing — Server

- [Unit Testing](#unit-testing)
- [Integration Testing](#integration-testing)
  - [Api Integration](#api-integration)
    - [Test Application](#test-application)
    - [Base Integration Test](#base-integration-test)
    - [App Http Client](#app-http-client)
    - [Test Auth Handler](#test-auth-handler)
    - [Collections](#collections)

Tests for the server cover two levels: unit tests for isolated classes, and integration tests that exercise the HTTP layer against a real Postgres instance.

Shared conventions (naming, Arrange/Act/Assert, fixtures) are described in the [overview](Testing.md#conventions).

## Unit Testing

Unit tests exercise a single class or function in isolation, with no database and no HTTP. They are fast, easy to write, and catch bugs early in the development cycle.

To run all unit tests in the server module:

```sh
./gradlew :planer-server:test
```

Unit tests can also be run directly from IntelliJ IDEA by clicking the gutter icon next to any test.

**Conventions specific to the server:**

- The class under test is held in a field named **`uut`**
- Plain data classes and DTOs are not unit-tested
- Dependencies are stubbed with hand-written fakes, not mocked

Example:

```kotlin
class RegisterUserServiceTest {

    private val userRepository = InMemoryUserRepository()
    private val passwordHasher = FakePasswordHasher()
    private val uut = RegisterUserService(userRepository, passwordHasher)

    @Test
    fun whenRegisterIsSuccessful_shouldCreateUserAndReturnToken() = runTest {
        // Arrange
        val command = RegisterUserCommand(
            email = "alex@example.com",
            password = "Password123!",
            displayName = "Alex Morgan",
            timezone = "Europe/Berlin"
        )

        // Act
        val result = uut.register(command)

        // Assert
        result.token.shouldNotBeNull()
        userRepository.findByEmail("alex@example.com").shouldNotBeNull()
    }
}
```

## Integration Testing

Integration tests exercise the server through its HTTP layer against a **real Postgres instance** running in a **Testcontainers** container.

Each test collection starts its own container, applies the migrations, and resets state between test methods via a **Resetter**. The server is started with the **Ktor Test Application** helper, so no real port is bound and no external process is required.

To run all integration tests:

```sh
./gradlew :planer-server:integrationTest
```

**Conventions specific to the server:**

- The system under test is held in a field named **`sut`**
- Test classes live in `test/integration`, grouped by collection
- Database state is reset per method, not per class

### Api Integration

The HTTP surface is tested by sending real requests through a **Ktor test client** against the **Test Application**.

The supporting classes live in `planer-server/src/test/kotlin/core`:

- **Test Application** — builds the Ktor app with test configuration
- **Base Integration Test** — provides the client and manages per-test state
- **App Http Client** — a thin wrapper around the Ktor client with helpers for auth
- **Test Auth Handler** — injects a JWT for tests without going through login
- **ITestData** — an interface for seeding datasets

API integration tests do not use the `uut` / `sut` convention — they use the **`client`** directly. There is no setup region; everything is provided by the base class.

#### Test Application

The **Test Application** starts the Ktor server in-process for testing. It:

- Replaces the real database context with a connection to the **Testcontainers** Postgres instance
- Loads `application-test.conf` instead of `application.conf`
- Installs the **Test Auth Handler** so tests can authenticate without hitting the login endpoint
- Exposes the `Application` object for the Ktor test client to connect to

The container itself is started once per **collection**, not once per class.

#### Base Integration Test

Every integration test class extends this. It:

- Opens a Ktor test client against the Test Application
- Exposes it as a `client` field
- Provides access to the database handle for direct assertions
- Resets the database state after each test method using **Resetter**
- Seeds data at the start of the class from an `ITestData` implementation

Resetting per method (rather than per class) keeps tests independent and makes failures reproducible.

#### App Http Client

The **App Http Client** wraps the Ktor test client and adds convenience methods:

- `get<T>(path)` / `post<T>(path, body)` / `put<T>` / `delete<T>`
- `withToken(userId)` — attaches a JWT for the given user for the rest of the test
- `clearToken()` — removes the current token

Example:

```kotlin
val response = client.withToken(alex.id).get<UserResponse>("/api/users/me")
```

#### Test Auth Handler

The **Test Auth Handler** is installed by the Test Application to bypass the normal JWT flow during tests.

Instead of validating a real signature, it reads a test-only header (`X-Test-User-Id`) and injects the corresponding `sub` claim into the request. This lets tests switch users without a real login round-trip.

The header is set automatically by `AppHttpClient.withToken(...)`.

#### Collections

Integration tests are grouped into **JUnit 5 test collections**, each backed by its own Postgres container. The container is started once per collection and shared across all test classes in it.

To create a collection:

```kotlin
class UserCollection : TestCollection(UserTestData())

abstract class UserTestData : ITestData {
    override fun seed(): SeedData = seedOf(
        users = listOf(alexMorgan, samLee)
    )
}
```

Every test class in the collection extends `BaseIntegrationTest` and uses the same container. The **Resetter** wipes the tables between test methods.

One container per collection, reset per method — fast test runs while keeping tests isolated.