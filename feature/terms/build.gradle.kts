plugins {
    id("tayra.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.ui)
        }
    }
}
