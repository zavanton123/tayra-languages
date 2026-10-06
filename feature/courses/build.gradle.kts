plugins {
    id("tayra.kmp.feature")
}

kotlin {
    // The sample courses are Compose resources, which an Android library only packages with this on.
    androidLibrary {
        androidResources {
            enable = true
        }
    }
    sourceSets {
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
