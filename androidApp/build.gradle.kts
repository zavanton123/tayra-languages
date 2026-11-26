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
    implementation(libs.android.billing.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
}

// Release builds pass the version from the git tag; local builds fall back to these values.
val releaseVersion = providers.gradleProperty("releaseVersion").orElse("0.1.0")
val releaseVersionCode = providers.gradleProperty("versionCode").map { it.toInt() }.orElse(1)

android {
    namespace = "com.idiomiko.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.idiomiko"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = releaseVersionCode.get()
        versionName = releaseVersion.get()
        // The speech and translation engines ship native code; only 64-bit ARM phones get it, which leaves out
        // the x86 copies and the 32-bit ones for the rare phones older than about 2017.
        ndk { abiFilters += setOf("arm64-v8a") }
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
    // The frequency lists (77 MB of text, 18 MB packed) are fetched for the language being learned instead of shipped:
    // see DownloadedFrequencyLists. The rest of the pattern is the default.
    androidResources {
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:!CVS:!thumbs.db:!picasa.ini:!*~:!*.tsv"
    }
    packaging {
        // The native engines (60 MB) are compressed in the APK, about halving the download; Android unpacks them on install.
        jniLibs { useLegacyPackaging = true }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Tesseract's bytedeco jars each carry GraalVM native-image settings and their C build files.
            excludes += "/META-INF/native-image/**"
            excludes += "/lib/*/lib/**"
            excludes += "/lib/*/include/**"
            excludes += "/lib/*/tesseract"
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
