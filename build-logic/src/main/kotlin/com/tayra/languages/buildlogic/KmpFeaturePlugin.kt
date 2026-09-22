package com.tayra.languages.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A feature module: Compose UI + view models, wired with Koin, depending on the core modules.
 */
class KmpFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("tayra.kmp.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.commonMain.dependencies {
                implementation(project(":core:domain"))
                implementation(project(":core:data"))
                implementation(project(":core:ui"))
                implementation(libs.findLibrary("jetbrains-navigation-compose").get())
                implementation(libs.findLibrary("koin-core").get())
                implementation(libs.findLibrary("koin-compose").get())
                implementation(libs.findLibrary("koin-compose-viewmodel").get())
                implementation(libs.findLibrary("kotlinx-coroutines-core").get())
                implementation(libs.findLibrary("kotlinx-collections-immutable").get())
                implementation(libs.findLibrary("kermit").get())
                implementation(libs.findLibrary("filekit-dialogs-compose").get())
                implementation(libs.findLibrary("coil-compose").get())
                implementation(libs.findLibrary("coil-network-ktor").get())
            }
        }
    }
}
