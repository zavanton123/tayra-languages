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
            id = "idiomiko.kmp.library"
            implementationClass = "com.idiomiko.buildlogic.KmpLibraryPlugin"
        }
        register("kmpCompose") {
            id = "idiomiko.kmp.compose"
            implementationClass = "com.idiomiko.buildlogic.KmpComposePlugin"
        }
        register("kmpFeature") {
            id = "idiomiko.kmp.feature"
            implementationClass = "com.idiomiko.buildlogic.KmpFeaturePlugin"
        }
    }
}
