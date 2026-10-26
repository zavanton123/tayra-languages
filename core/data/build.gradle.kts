plugins {
    id("tayra.kmp.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("TayraDatabase") {
            packageName.set("com.tayra.languages.core.data.db")
            generateAsync.set(true)
        }
        // Read-only dictionaries shipped as prebuilt SQLite files (tools/build_english_russian_dictionary.py).
        create("DictionaryDatabase") {
            packageName.set("com.tayra.languages.core.data.dictionary")
            srcDirs.setFrom("src/commonMain/sqldelight-dictionary")
            generateAsync.set(true)
        }
        // Read-only course packs downloaded from Settings > Courses (tools/build_courses.py).
        create("CoursePackDatabase") {
            packageName.set("com.tayra.languages.core.data.coursepack")
            srcDirs.setFrom("src/commonMain/sqldelight-coursepack")
            generateAsync.set(true)
        }
        // Read-only book packs downloaded from Settings > Books (tools/build_books.py).
        create("BookPackDatabase") {
            packageName.set("com.tayra.languages.core.data.bookpack")
            srcDirs.setFrom("src/commonMain/sqldelight-bookpack")
            generateAsync.set(true)
        }
    }
}

/**
 * Tesseract's native libraries for the computer this builds on: each desktop installer is built on
 * its own system (the release builds a macOS arm64, a Windows x64 and a Linux x64 one).
 */
val javacppDesktopPlatform: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arm = System.getProperty("os.arch") in setOf("aarch64", "arm64")
    when {
        "mac" in os -> if (arm) "macosx-arm64" else "macosx-x86_64"
        "win" in os -> "windows-x86_64"
        else -> if (arm) "linux-arm64" else "linux-x86_64"
    }
}

/** The bytedeco libraries Tesseract needs: its own, Leptonica (images) and JavaCPP (the bridge), with natives for [platform]. */
fun tesseractNatives(platform: String): List<String> = listOf(
    "org.bytedeco:javacpp:${libs.versions.bytedeco.javacpp.get()}:$platform",
    "org.bytedeco:leptonica:${libs.versions.bytedeco.leptonica.get()}:$platform",
    "org.bytedeco:tesseract:${libs.versions.bytedeco.tesseract.get()}:$platform",
)

kotlin {
    applyDefaultHierarchyTemplate()

    sourceSets {
        val jvmShared by creating { dependsOn(commonMain.get()) }
        androidMain.get().dependsOn(jvmShared)
        jvmMain.get().dependsOn(jvmShared)
        jvmShared.dependencies {
            implementation(libs.bytedeco.javacpp)
            implementation(libs.bytedeco.leptonica)
            implementation(libs.bytedeco.tesseract)
        }

        commonMain.dependencies {
            api(projects.core.domain)
            implementation(projects.syncProtocol)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.async.extensions)
            implementation(libs.sqldelight.primitive.adapters)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.kermit)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.mlkit.translate)
            implementation("k2-fsa:sherpa-onnx:1.13.8@aar")
            implementation(libs.commons.compress)
            implementation(libs.pdfbox.android)
            implementation(libs.mlkit.text.latin)
            implementation(libs.mlkit.text.chinese)
            implementation(libs.mlkit.text.japanese)
            implementation(libs.mlkit.text.korean)
            implementation(libs.mlkit.text.devanagari)
            implementation(libs.play.services.base)
            // Bytedeco builds Tesseract for 64-bit ARM only; older 32-bit phones read photos with ML Kit alone.
            tesseractNatives("android-arm64").forEach { implementation(it) }
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.jna)
            implementation(libs.jna.platform)
            implementation(libs.kuromoji.ipadic)
            implementation(libs.pdfbox)
            tesseractNatives(javacppDesktopPlatform).forEach { implementation(it) }
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
            implementation(libs.ktor.client.darwin)
        }
        wasmJsMain.dependencies {
            implementation(libs.sqldelight.web.worker.driver)
            implementation(libs.kotlinx.browser)
            implementation(libs.ktor.client.js)
            implementation(npm("sql.js", "1.13.0"))
            // Piper in the browser; its ONNX runtime binaries come from a CDN pinned to 1.18.0, so the library must match.
            implementation(npm("@mintplex-labs/piper-tts-web", "1.0.5"))
            implementation(npm("onnxruntime-web", "1.18.0"))
            // Kokoro in the browser; its non-English voices are phonemized with the eSpeak build Piper ships.
            implementation(npm("kokoro-js", "1.2.1"))
        }
        jvmTest.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.ktor.client.mock)
            implementation(libs.multiplatform.settings.test)
        }
    }
}
