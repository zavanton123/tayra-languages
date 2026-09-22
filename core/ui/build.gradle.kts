plugins {
    id("tayra.kmp.compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(libs.jetbrains.navigation.compose)
            api(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
