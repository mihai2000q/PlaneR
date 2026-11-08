plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.flyway)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

ktor {
    fatJar {
        archiveFileName = "planer-server-all.jar"
    }
}

// Migrations are not applied by the server: run them with `./gradlew :planer-server:flywayMigrate`
// (or the `migrate` service in docker-compose.yml). Connection values mirror application.conf / .env.dev.
flyway {
    val host = System.getenv("DB_HOST") ?: "localhost"
    val port = System.getenv("DB_PORT") ?: "5432"
    val name = System.getenv("DB_NAME") ?: "planer"
    url = "jdbc:postgresql://$host:$port/$name"
    user = System.getenv("DB_USER") ?: "planer"
    password = System.getenv("DB_PASSWORD") ?: "planer"
    locations = arrayOf("filesystem:$projectDir/src/main/resources/db/migration")
    // `flywayClean` drops everything in the database. Allowed here because this is the development setup;
    // production runs migrations through its own pipeline and never through this block.
    cleanDisabled = false
}

dependencies {
    implementation(project(":planer-shared"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.di)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback.classic)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.java.time)
    implementation(libs.postgresql)
    implementation(libs.hikari)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
