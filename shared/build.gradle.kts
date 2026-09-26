plugins {
    id("tayra.kmp.compose")
    alias(libs.plugins.kotlin.serialization)
}

compose.resources {
    packageOfResClass = "com.tayra.languages.shared.resources"
    generateResClass = always
}

kotlin {
    // Compose resources reach the Android app as assets, which the AGP KMP plugin only packs
    // when Android resources are enabled for the module.
    androidLibrary {
        androidResources.enable = true
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            api(projects.core.data)
            api(projects.core.ui)
            implementation(projects.feature.books)
            implementation(projects.feature.languages)
            implementation(projects.feature.terms)
            implementation(projects.feature.reading)
            implementation(projects.feature.settings)
            implementation(projects.feature.stats)
            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.compose.components.resources)
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kermit)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}
