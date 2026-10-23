plugins {
    id("tayra.kmp.feature")
}

kotlin {
    // The notification's cover is a Compose resource, which an Android library only packages with this on.
    androidLibrary {
        androidResources {
            enable = true
        }
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.media3.session)
            implementation(libs.androidx.activity.compose)
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
