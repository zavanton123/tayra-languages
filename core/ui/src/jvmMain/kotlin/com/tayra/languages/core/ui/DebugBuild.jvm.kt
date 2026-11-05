package com.tayra.languages.core.ui

/** Only an installed app has this launcher's path; a run from the sources does not. */
actual val isDebugBuild: Boolean = System.getProperty("jpackage.app-path") == null
