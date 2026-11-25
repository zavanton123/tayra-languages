package com.idiomiko.core.ui.components

import androidx.compose.runtime.Composable
import com.idiomiko.core.ui.i18n.tr

/** The running app's version, as its GitHub release is named ("0.11.2"); null where the build carries none, as on the web or in a run from the sources. */
@Composable
expect fun appVersion(): String?

/**
 * In a run from the sources, the release it was built after and how far past it, as `git describe`
 * names it ("0.19.0-77-g12599fb"); null in a released build, and where there are no sources to name.
 */
expect fun sourceVersion(): String?

/** "Version 0.19.0", or for a run from the sources "Version 0.19.0-77-g12599fb (from the sources)"; null where neither is known. */
@Composable
fun versionLabel(): String? =
    appVersion()?.let { tr("Version {0}", it) } ?: sourceVersion()?.let { tr("Version {0} (from the sources)", it) }
