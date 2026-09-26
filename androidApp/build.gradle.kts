import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.core)
    implementation(libs.filekit.dialogs.compose)
}

// Release builds pass the version from the git tag; local builds fall back to these values.
val releaseVersion = providers.gradleProperty("releaseVersion").orElse("0.1.0")
val releaseVersionCode = providers.gradleProperty("versionCode").map { it.toInt() }.orElse(1)

android {
    namespace = "com.tayra.languages.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.tayra.languages"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = releaseVersionCode.get()
        versionName = releaseVersion.get()
    }
    signingConfigs {
        // A keystore from the environment signs published releases; without one the debug key
        // is used so that the release APK still installs.
        val keystore = providers.environmentVariable("ANDROID_KEYSTORE_FILE").orNull
        if (!keystore.isNullOrBlank()) {
            create("release") {
                storeFile = file(keystore)
                storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
