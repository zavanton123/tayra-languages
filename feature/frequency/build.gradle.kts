plugins {
    id("tayra.kmp.feature")
}

kotlin {
    // The frequency lists are Compose resources, which an Android library only packages with this on.
    androidLibrary {
        androidResources {
            enable = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.terms)
        }
        jvmTest.dependencies {
            implementation(projects.core.data)
            implementation(libs.kotlinx.coroutines.test)
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
