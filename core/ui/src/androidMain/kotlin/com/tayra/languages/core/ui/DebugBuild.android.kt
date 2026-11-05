package com.tayra.languages.core.ui

/** Set by the application as it starts: only the app knows how it was built, a library does not. */
var androidDebuggable: Boolean = false

actual val isDebugBuild: Boolean get() = androidDebuggable
