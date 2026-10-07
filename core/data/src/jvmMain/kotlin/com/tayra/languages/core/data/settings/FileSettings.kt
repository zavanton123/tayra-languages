package com.tayra.languages.core.data.settings

import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import java.io.File
import java.util.Properties

/** Settings kept in a properties file, written again after every change. */
fun fileSettings(file: File): Settings {
    val properties = Properties()
    if (file.exists()) file.inputStream().use(properties::load)
    return PropertiesSettings(properties) { changed ->
        file.parentFile?.mkdirs()
        file.outputStream().use { changed.store(it, null) }
    }
}
