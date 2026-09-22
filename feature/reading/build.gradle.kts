plugins {
    id("tayra.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.terms)
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test.junit4)
            implementation(libs.junit4)
            implementation(libs.compose.foundation)
            implementation(compose.desktop.currentOs)
        }
    }
}
