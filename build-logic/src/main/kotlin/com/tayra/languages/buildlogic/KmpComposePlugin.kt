package com.tayra.languages.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A KMP library that also ships shared Compose UI.
 */
class KmpComposePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("tayra.kmp.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.commonMain.dependencies {
                implementation(libs.findLibrary("compose-runtime").get())
                implementation(libs.findLibrary("compose-foundation").get())
                implementation(libs.findLibrary("compose-material3").get())
                implementation(libs.findLibrary("compose-ui").get())
                implementation(libs.findLibrary("compose-components-resources").get())
                implementation(libs.findLibrary("compose-material-icons-core").get())
                implementation(libs.findLibrary("jetbrains-lifecycle-runtime-compose").get())
                implementation(libs.findLibrary("jetbrains-lifecycle-viewmodel-compose").get())
                implementation(libs.findLibrary("kotlinx-coroutines-core").get())
            }
        }
    }
}
