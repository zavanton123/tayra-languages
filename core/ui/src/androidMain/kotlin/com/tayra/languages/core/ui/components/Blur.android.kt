package com.tayra.languages.core.ui.components

import android.os.Build

// Compose blurs only with the render effects of Android 12.
actual val softBlurAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
