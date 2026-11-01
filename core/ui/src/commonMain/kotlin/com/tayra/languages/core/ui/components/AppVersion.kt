package com.tayra.languages.core.ui.components

import androidx.compose.runtime.Composable

/** The running app's version, as its GitHub release is named ("0.11.2"); null where the build carries none, as on the web or in a run from the sources. */
@Composable
expect fun appVersion(): String?
