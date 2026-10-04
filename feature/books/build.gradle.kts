plugins {
    id("tayra.kmp.feature")
}

kotlin {
    sourceSets {
        jvmTest.dependencies {
            implementation(libs.compose.ui.test.junit4)
            implementation(libs.junit4)
            implementation(compose.desktop.currentOs)
        }
    }
}
