plugins {
    id("tayra.kmp.compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // The bundled reading fonts are Compose resources, which an Android library only packages with this on.
    androidLibrary {
        androidResources {
            enable = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(libs.jetbrains.navigation.compose)
            api(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kermit)
            implementation(libs.filekit.dialogs.compose)
        }
        jvmMain.dependencies {
            implementation(libs.jlayer)
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test.junit4)
            implementation(libs.junit4)
            implementation(compose.desktop.currentOs)
        }
    }
}
