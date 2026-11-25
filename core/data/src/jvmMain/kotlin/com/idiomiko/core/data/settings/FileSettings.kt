package com.idiomiko.core.data.settings

import com.russhwolf.settings.PropertiesSettings
import com.russhwolf.settings.Settings
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
import java.util.prefs.Preferences

/** The name of the settings file in a data folder. */
const val SETTINGS_FILE = "settings.properties"

/**
 * The desktop app's settings: a file of its own in [folder], which no other program writes. The first
 * time there is none, it starts from the preferences [former] gives, where the app kept them before.
 */
fun desktopSettings(folder: File, former: () -> Preferences?): Settings {
    val file = File(folder, SETTINGS_FILE)
    if (!file.exists()) {
        former()?.let { preferences ->
            val values = Properties()
            for (key in preferences.keys()) preferences.get(key, null)?.let { values.setProperty(key, it) }
            if (!values.isEmpty) save(values, file)
        }
    }
    return fileSettings(file)
}

/**
 * Settings kept in a properties file. Each change, or each batch of them, replaces the file whole, so
 * a crash leaves the old file or the new one and never part of either.
 */
fun fileSettings(file: File): Settings {
    val properties = Properties()
    if (file.exists()) file.inputStream().use(properties::load)
    val writer = SettingsFileWriter(file, properties)
    return PropertiesFileSettings(writer, properties)
}

private class PropertiesFileSettings(private val writer: SettingsFileWriter, properties: Properties) :
    BatchedSettings, Settings by PropertiesSettings(properties, { writer.changed() }) {
    override fun batch(block: () -> Unit) = writer.batch(block)
}

/** Saves the file after each change, or once at the end of a batch of them. */
private class SettingsFileWriter(private val file: File, private val properties: Properties) {
    private var depth = 0
    private var pending = false

    @Synchronized
    fun changed() {
        if (depth > 0) pending = true else save(properties, file)
    }

    @Synchronized
    fun batch(block: () -> Unit) {
        depth++
        try {
            block()
        } finally {
            depth--
            if (depth == 0 && pending) {
                pending = false
                save(properties, file)
            }
        }
    }
}

/** Writes [values] beside [file] and moves them into its place in one step. */
private fun save(values: Properties, file: File) {
    val target = file.absoluteFile
    target.parentFile.mkdirs()
    val temp = File(target.parentFile, "${target.name}.saving")
    temp.outputStream().use { values.store(it, null) }
    Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
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
