package com.idiomiko.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** ":core:domain" -> "com.idiomiko.core.domain" */
internal fun Project.androidNamespace(): String =
    "com.idiomiko" + path.replace(':', '.').replace('-', '_')
