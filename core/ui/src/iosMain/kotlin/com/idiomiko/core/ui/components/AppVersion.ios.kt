package com.idiomiko.core.ui.components

import androidx.compose.runtime.Composable
import platform.Foundation.NSBundle

@Composable
actual fun appVersion(): String? = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String

actual fun sourceVersion(): String? = null
