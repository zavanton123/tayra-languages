// The wire format and merge rules of sync, shared by the apps and the sync service so the two cannot drift.
// The service (the private idiomiko-backend repository) keeps a copy of src/ and its CI fails when the copies
// differ: change the protocol here first, copy it there, and deploy the service before releasing the apps.
plugins {
    id("idiomiko.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
        }
    }
}
