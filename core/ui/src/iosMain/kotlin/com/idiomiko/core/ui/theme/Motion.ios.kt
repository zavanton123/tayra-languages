package com.idiomiko.core.ui.theme

import androidx.compose.runtime.Composable
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
internal actual fun systemReducesMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()
