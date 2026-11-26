plugins {
    id("idiomiko.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.json)
        }
    }
}

// BundledFrequencyListsTest reads these, so a change to them runs it again.
tasks.named<Test>("jvmTest") {
    inputs.file(rootProject.file("androidApp/bundled-frequency-lists.txt")).withPropertyName("bundledFrequencyLists")
    inputs.dir(rootProject.file("feature/frequency/src/commonMain/composeResources/files/frequency")).withPropertyName("frequencyLists")
}
