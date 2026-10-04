package com.tayra.languages.core.data.settings

import com.russhwolf.settings.Settings

/** [Settings] kept as text in a map, the form they take in a backup. */
internal class StringMapSettings(private val map: MutableMap<String, String> = mutableMapOf()) : Settings {
    val values: Map<String, String> get() = map.toMap()

    override val keys: Set<String> get() = map.keys.toSet()
    override val size: Int get() = map.size
    override fun clear() = map.clear()
    override fun remove(key: String) { map.remove(key) }
    override fun hasKey(key: String): Boolean = key in map

    override fun putInt(key: String, value: Int) { map[key] = value.toString() }
    override fun getInt(key: String, defaultValue: Int): Int = getIntOrNull(key) ?: defaultValue
    override fun getIntOrNull(key: String): Int? = map[key]?.toIntOrNull()

    override fun putLong(key: String, value: Long) { map[key] = value.toString() }
    override fun getLong(key: String, defaultValue: Long): Long = getLongOrNull(key) ?: defaultValue
    override fun getLongOrNull(key: String): Long? = map[key]?.toLongOrNull()

    override fun putString(key: String, value: String) { map[key] = value }
    override fun getString(key: String, defaultValue: String): String = map[key] ?: defaultValue
    override fun getStringOrNull(key: String): String? = map[key]

    override fun putFloat(key: String, value: Float) { map[key] = value.toString() }
    override fun getFloat(key: String, defaultValue: Float): Float = getFloatOrNull(key) ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = map[key]?.toFloatOrNull()

    override fun putDouble(key: String, value: Double) { map[key] = value.toString() }
    override fun getDouble(key: String, defaultValue: Double): Double = getDoubleOrNull(key) ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = map[key]?.toDoubleOrNull()

    override fun putBoolean(key: String, value: Boolean) { map[key] = value.toString() }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = getBooleanOrNull(key) ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = map[key]?.toBooleanStrictOrNull()
}
