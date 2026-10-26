package com.tayra.languages.core.data.account

import android.os.Build

internal actual fun deviceLabel(): DeviceLabel {
    val model = Build.MODEL.orEmpty()
    val name = if (model.startsWith(Build.MANUFACTURER.orEmpty(), ignoreCase = true)) model else "${Build.MANUFACTURER} $model"
    return DeviceLabel(name.trim(), "android")
}

internal actual fun defaultAccountServer(): String? = null
