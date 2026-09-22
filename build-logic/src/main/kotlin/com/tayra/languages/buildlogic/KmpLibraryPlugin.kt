package com.tayra.languages.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configures a Kotlin Multiplatform library module with the four app targets:
 * Android, JVM (desktop), iOS and Web (wasm).
 */
class KmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        extensions.configure<KotlinMultiplatformExtension> {
            configureTargets(this@with)

            sourceSets.commonTest.dependencies {
                implementation(libs.findLibrary("kotlin-test").get())
                implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                implementation(libs.findLibrary("turbine").get())
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    private fun KotlinMultiplatformExtension.configureTargets(project: Project) {
        jvm()
        iosArm64()
        iosSimulatorArm64()
        wasmJs { browser() }

        (this as org.gradle.api.plugins.ExtensionAware).extensions
            .configure<KotlinMultiplatformAndroidLibraryTarget>("androidLibrary") {
                namespace = project.androidNamespace()
                compileSdk = project.libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
                minSdk = project.libs.findVersion("android-minSdk").get().requiredVersion.toInt()
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }

        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
}
