plugins {
    id("tayra.kmp.feature")
}

kotlin {
    sourceSets {
        jvmTest.dependencies {
            implementation(projects.core.data)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.compose.ui.test.junit4)
            implementation(libs.junit4)
            implementation(libs.compose.foundation)
            implementation(compose.desktop.currentOs)
        }
    }
}
