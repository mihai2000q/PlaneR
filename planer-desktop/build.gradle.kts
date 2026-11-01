import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.desktop)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":planer-shared"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.kotlinx.coroutines.swing)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotest.assertions.core)
    testImplementation(compose.desktop.uiTestJUnit4)
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "app.planer.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "PlaneR"
            // Installers require a major version of at least 1, so the 0.x project version cannot be used here.
            packageVersion = "1.0.0"
        }
    }
}
