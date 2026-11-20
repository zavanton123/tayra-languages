plugins {
    id("idiomiko.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.ui.backhandler)
        }
        jvmTest.dependencies {
            implementation(projects.core.data)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.compose.ui.test.junit4)
            implementation(libs.junit4)
            implementation(compose.desktop.currentOs)
        }
    }
}
