// The wire format and merge rules of sync, shared by the apps and the sync backend so the two cannot drift.
plugins {
    id("tayra.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
        }
    }
}
