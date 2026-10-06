rootProject.name = "tayra-languages"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // sherpa-onnx publishes its Android library only as a release asset.
        ivy {
            url = uri("https://github.com/k2-fsa/sherpa-onnx/releases/download/")
            patternLayout { artifact("v[revision]/[module]-[revision].[ext]") }
            metadataSources { artifact() }
            content { includeGroup("k2-fsa") }
        }
    }
}

include(":core:domain")
include(":core:data")
include(":core:ui")
include(":feature:books")
include(":feature:courses")
include(":feature:frequency")
include(":feature:flashcards")
include(":feature:languages")
include(":feature:reading")
include(":feature:settings")
include(":feature:stats")
include(":feature:terms")
include(":shared")
include(":androidApp")
include(":desktopApp")
include(":webApp")
