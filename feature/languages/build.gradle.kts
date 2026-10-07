plugins {
    id("tayra.kmp.feature")
}

kotlin {
    sourceSets {
        jvmTest.dependencies {
            implementation(projects.core.data)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.compose.ui.test.junit4)
            implementation(libs.junit4)
            implementation(compose.desktop.currentOs)
        }
    }
}
