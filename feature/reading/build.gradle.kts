plugins {
    id("tayra.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.terms)
        }
    }
}
