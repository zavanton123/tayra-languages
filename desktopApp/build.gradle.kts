import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(projects.shared)
    // The `tayra` command, packaged as a second launcher of the app's own jars.
    implementation(projects.cli)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.filekit.dialogs.compose)
}

// macOS installers need MAJOR.MINOR.PATCH with a major version of at least 1, so tags below
// 1.0.0 are packaged as 1.0.0 with the real version kept in the file name by the release workflow.
val desktopVersion = providers.gradleProperty("releaseVersion")
    .map { if (Regex("[1-9]\\d*\\.\\d+\\.\\d+").matches(it)) it else "1.0.0" }
    .orElse("1.0.0")

// Windows, Debian and RPM installers accept versions from 0.0.0 up, so they keep the real version and each release upgrades the last.
val installerVersion = providers.gradleProperty("releaseVersion")
    .map { if (Regex("\\d+\\.\\d+\\.\\d+").matches(it)) it else "1.0.0" }
    .orElse("1.0.0")

// -Xdock is a macOS launcher option; the JVM on other systems refuses to start with it.
val isMacHost = System.getProperty("os.name").lowercase().contains("mac")
val dockNameArgs = if (isMacHost) listOf("-Xdock:name=Tayra Languages", "-Dtayra.dockNamed=true") else emptyList()

// Android Studio injects Compose Hot Reload's `hotRun` task; give that JVM the Dock name too.
tasks.matching { it.name == "hotRun" }.configureEach {
    (this as? JavaExec)?.jvmArgs(dockNameArgs)
}

compose.desktop {
    application {
        mainClass = "com.tayra.languages.desktop.MainKt"
        // Names the process in the macOS Dock during development runs; packaged apps use the bundle name.
        jvmArgs += dockNameArgs

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "TayraLanguages"
            packageVersion = desktopVersion.get()
            // The bundled runtime only has the JDK modules listed here; without java.sql the database cannot open.
            // `./gradlew :desktopApp:suggestRuntimeModules` lists what the dependencies need.
            modules("java.instrument", "java.management", "java.net.http", "java.prefs", "java.sql", "jdk.security.auth", "jdk.unsupported")
            vendor = "Tayra Languages"
            description = "Read foreign-language texts and learn their words"
            macOS { iconFile.set(project.file("icons/TayraLanguages.icns")) }
            windows {
                iconFile.set(project.file("icons/TayraLanguages.ico"))
                msiPackageVersion = installerVersion.get()
                // Kept for good: Windows replaces an installed version only when the new MSI has the same upgrade code.
                upgradeUuid = "f8e8f40e-d1ad-4d01-ae94-eb98d11cfe66"
                perUserInstall = true
                menu = true
                menuGroup = "Tayra Languages"
                shortcut = true
                dirChooser = true
            }
            linux {
                iconFile.set(project.file("icons/TayraLanguages.png"))
                // Debian and RPM package names are lower case; the installed app keeps its display name.
                packageName = "tayra-languages"
                debPackageVersion = installerVersion.get()
                rpmPackageVersion = installerVersion.get()
                appCategory = "Education"
                menuGroup = "Education"
                shortcut = true
            }
        }
    }
}

// The app loads classes lazily from the modules' build/libs jars, and Gradle rewrites those in place, so any build
// while it was open broke the next screen it drew with NoClassDefFoundError. Runs start from a private copy instead;
// replacing the copy only unlinks the old files, which an app that is still open keeps reading.
tasks.withType<JavaExec>().matching { it.name == "run" }.configureEach {
    // A run from the sources has no bundled `tayra`; Settings > Command-line tool puts the one :cli builds on the PATH instead.
    dependsOn(":cli:installDist")
    val windowsHost = System.getProperty("os.name").lowercase().contains("win")
    systemProperty("tayra.cliLauncher", rootDir.resolve("cli/build/install/tayra/bin/" + if (windowsHost) "tayra.bat" else "tayra").absolutePath)
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

// The installed app carries a second launcher, `tayra`, that runs the command line of :cli on the app's own
// runtime and jars: a console program on Windows, and headless so macOS shows no Dock icon for it.
// Settings > Command-line tool puts it on the PATH.
val cliLauncherProperties = layout.buildDirectory.file("cli-launcher/tayra.properties")
val writeCliLauncherProperties by tasks.registering {
    val target = cliLauncherProperties
    val windowsIcon = project.file("icons/TayraLanguages.ico").absolutePath.replace("\\", "/")
    outputs.file(target)
    doLast {
        target.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                listOf(
                    "main-class=com.tayra.languages.cli.MainKt",
                    "java-options=-Djava.awt.headless=true",
                    "win-console=true",
                    "win-menu=false",
                    "win-shortcut=false",
                    "linux-shortcut=false",
                ).plus(if (System.getProperty("os.name").lowercase().contains("win")) listOf("icon=$windowsIcon") else emptyList())
                    .joinToString("\n", postfix = "\n"),
            )
        }
    }
}
// Only the app image is made with jpackage's own inputs; the installers are packed from that image.
tasks.withType<AbstractJPackageTask>().matching { it.name.startsWith("create") && it.name.endsWith("Distributable") }.configureEach {
    dependsOn(writeCliLauncherProperties)
    freeArgs.addAll("--add-launcher", "tayra=${cliLauncherProperties.get().asFile.absolutePath}")
}
