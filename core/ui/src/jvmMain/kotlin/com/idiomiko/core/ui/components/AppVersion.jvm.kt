package com.idiomiko.core.ui.components

import androidx.compose.runtime.Composable

/** Set by the installers' launcher from the release's version, as the updater reads it. */
@Composable
actual fun appVersion(): String? = System.getProperty("idiomiko.version")

/** Set by the build for a run from the sources, from `git describe`. */
actual fun sourceVersion(): String? = System.getProperty("idiomiko.sourceVersion")?.takeIf { it.isNotBlank() }
