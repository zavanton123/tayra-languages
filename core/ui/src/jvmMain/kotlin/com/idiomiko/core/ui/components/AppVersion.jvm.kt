package com.idiomiko.core.ui.components

import androidx.compose.runtime.Composable

/** Set by the installers' launcher from the release's version, as the updater reads it. */
@Composable
actual fun appVersion(): String? = System.getProperty("idiomiko.version")
