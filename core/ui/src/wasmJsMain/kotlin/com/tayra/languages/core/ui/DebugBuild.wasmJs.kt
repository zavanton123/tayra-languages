package com.tayra.languages.core.ui

import kotlinx.browser.window

/** The development server is on this machine; the published site never is. */
actual val isDebugBuild: Boolean = window.location.hostname.let { it == "localhost" || it == "127.0.0.1" }
