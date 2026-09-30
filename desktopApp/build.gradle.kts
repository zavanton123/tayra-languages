import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.filekit.dialogs.compose)
}

// macOS installers need MAJOR.MINOR.PATCH with a major version of at least 1, so tags below
// 1.0.0 are packaged as 1.0.0 with the real version kept in the file name by the release workflow.
val desktopVersion = providers.gradleProperty("releaseVersion")
    .map { if (Regex("[1-9]\\d*\\.\\d+\\.\\d+").matches(it)) it else "1.0.0" }
    .orElse("1.0.0")

// Android Studio injects Compose Hot Reload's `hotRun` task; give that JVM the Dock name too.
tasks.matching { it.name == "hotRun" }.configureEach {
    (this as? JavaExec)?.jvmArgs("-Xdock:name=Tayra Languages", "-Dtayra.dockNamed=true")
}

compose.desktop {
    application {
        mainClass = "com.tayra.languages.desktop.MainKt"
        // Names the process in the macOS Dock during development runs; packaged apps use the bundle name.
        jvmArgs += listOf("-Xdock:name=Tayra Languages", "-Dtayra.dockNamed=true")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TayraLanguages"
            packageVersion = desktopVersion.get()
            macOS { iconFile.set(project.file("icons/TayraLanguages.icns")) }
            windows { iconFile.set(project.file("icons/TayraLanguages.ico")) }
            linux { iconFile.set(project.file("icons/TayraLanguages.png")) }
        }
    }
}

// The app loads classes lazily from the modules' build/libs jars, and Gradle rewrites those in place, so any build
// while it was open broke the next screen it drew with NoClassDefFoundError. Runs start from a private copy instead;
// replacing the copy only unlinks the old files, which an app that is still open keeps reading.
tasks.withType<JavaExec>().matching { it.name == "run" }.configureEach {
    val projectRoot = rootDir.absolutePath + File.separator
    val copies = layout.buildDirectory.dir("run-classpath").get().asFile
    val fileCollections = objects
    doFirst {
        copies.deleteRecursively()
        copies.mkdirs()
        classpath = fileCollections.fileCollection().from(classpath.files.mapIndexed { i, file ->
            if (!file.isFile || !file.absolutePath.startsWith(projectRoot)) file
            else copies.resolve("$i-${file.name}").also { file.copyTo(it) }
        })
    }
}
