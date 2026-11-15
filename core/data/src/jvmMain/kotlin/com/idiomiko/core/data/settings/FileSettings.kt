package com.idiomiko.core.data.settings

import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import java.io.File
import java.util.Properties
import java.util.prefs.Preferences

/** Settings kept in a properties file, written again after every change. */
fun fileSettings(file: File): Settings {
    val properties = Properties()
    if (file.exists()) file.inputStream().use(properties::load)
    return PropertiesSettings(properties) { changed ->
        file.parentFile?.mkdirs()
        file.outputStream().use { changed.store(it, null) }
    }
}

/** The app's preferences; those of its former name, Tayra Languages, are carried over on first start. */
fun desktopPreferences(root: Preferences = Preferences.userRoot()): Preferences {
    val node = root.node("com/idiomiko")
    if (node.keys().isEmpty() && root.nodeExists("com/tayra/languages")) {
        val former = root.node("com/tayra/languages")
        for (key in former.keys()) former.get(key, null)?.let { node.put(key, it) }
        node.flush()
    }
    return node
}
