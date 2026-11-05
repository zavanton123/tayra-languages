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
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
            implementation(libs.androidx.media3.exoplayer.hls)
        }
        jvmMain.dependencies {
            implementation(libs.jlayer)
            // Videos play in Chromium, which JetBrains Runtime carries as its jcef module; this is only its API.
            compileOnly(libs.jcef.api.get().toString()) {
                exclude(group = "org.jogamp.gluegen")
                exclude(group = "org.jogamp.jogl")
            }
            implementation(libs.compose.ui.backhandler)
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test.junit4)
            implementation(libs.junit4)
            implementation(compose.desktop.currentOs)
        }
    }
}

// TranslationsTest reads the sources of every module that shows text, so they are inputs of the tests.
tasks.withType<Test>().configureEach {
    inputs.files(
        fileTree(rootDir) {
            include("core/ui/src/**/*.kt", "shared/src/**/*.kt", "feature/*/src/**/*.kt", "desktopApp/src/**/*.kt", "androidApp/src/**/*.kt", "webApp/src/**/*.kt")
            exclude("**/build/**")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("translatedSources")
}
