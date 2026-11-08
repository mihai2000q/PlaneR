buildscript {
    dependencies {
        // Flyway 10+ keeps database support in separate modules, and the Gradle plugin needs it on its own classpath.
        classpath(libs.flyway.database.postgresql)
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.desktop) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.flyway) apply false
}

allprojects {
    group = "app.planer"
    version = "0.1.0"
}
