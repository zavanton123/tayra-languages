plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradlePlugin.android)
    implementation(libs.gradlePlugin.compose)
    implementation(libs.gradlePlugin.composeCompiler)
    implementation(libs.gradlePlugin.kotlin)
    implementation(libs.gradlePlugin.kotlinSerialization)
    implementation(libs.gradlePlugin.sqldelight)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "tayra.kmp.library"
            implementationClass = "com.tayra.languages.buildlogic.KmpLibraryPlugin"
        }
        register("kmpCompose") {
            id = "tayra.kmp.compose"
            implementationClass = "com.tayra.languages.buildlogic.KmpComposePlugin"
        }
        register("kmpFeature") {
            id = "tayra.kmp.feature"
            implementationClass = "com.tayra.languages.buildlogic.KmpFeaturePlugin"
        }
    }
}
