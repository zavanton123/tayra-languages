package com.tayra.languages.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A KMP library that also ships shared Compose UI.
 */
class KmpComposePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("tayra.kmp.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val compose = extensions.getByType<ComposeExtension>().dependencies

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.commonMain.dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(libs.findLibrary("compose-material-icons-core").get())
                implementation(libs.findLibrary("jetbrains-lifecycle-runtime-compose").get())
                implementation(libs.findLibrary("jetbrains-lifecycle-viewmodel-compose").get())
            }
        }
    }
}

@Suppress("unused")
private val ComposePlugin.Dependencies.unused get() = Unit
