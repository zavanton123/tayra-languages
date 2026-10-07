plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

// The `tayra` command. It ships inside the desktop app as a second launcher of the same jars,
// and `./gradlew :cli:run --args="books list"` runs it from the sources.
dependencies {
    implementation(projects.core.data)
    implementation(projects.feature.frequency)
    implementation(libs.clikt)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kermit)
    // Ktor and others log through SLF4J, which warns on every start when no backend is present.
    runtimeOnly(libs.slf4j.nop)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit4)
}

application {
    mainClass = "com.tayra.languages.cli.MainKt"
    applicationName = "tayra"
}

// Some libraries reach the classpath twice under the same jar name, by way of their multiplatform variants.
distributions {
    main {
        contents { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
    }
}
