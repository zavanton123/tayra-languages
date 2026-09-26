import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.filekit.dialogs.compose)
}

// macOS installers need MAJOR.MINOR.PATCH with a major version of at least 1, so tags below
// 1.0.0 are packaged as 1.0.0 with the real version kept in the file name by the release workflow.
val desktopVersion = providers.gradleProperty("releaseVersion")
    .map { if (Regex("[1-9]\\d*\\.\\d+\\.\\d+").matches(it)) it else "1.0.0" }
    .orElse("1.0.0")

compose.desktop {
    application {
        mainClass = "com.tayra.languages.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TayraLanguages"
            packageVersion = desktopVersion.get()
        }
    }
}
