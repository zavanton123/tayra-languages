package com.tayra.languages.core.data.account

import platform.UIKit.UIDevice

internal actual fun deviceLabel(): DeviceLabel = DeviceLabel(UIDevice.currentDevice.name, "ios")

internal actual fun defaultAccountServer(): String? = null
