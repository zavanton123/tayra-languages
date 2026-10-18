package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/** A released version of the app, newer than the running one. */
data class AppUpdate(
    /** "0.6.0". */
    val version: String,
    /** The release's notes, as written on GitHub. */
    val notes: String,
    /** The release's page, where the installers can be downloaded by hand. */
    val pageUrl: String,
    /** The installer this copy of the app installs itself from; null when it cannot update itself (a run from the sources, a Mac with an Intel chip). */
    val installer: UpdateInstaller?,
)

/** A release's installer file. */
data class UpdateInstaller(val name: String, val url: String, val sizeBytes: Long, /** Its SHA-256 as GitHub records it, hex; null when GitHub gives none. */ val sha256: String?)

/**
 * The desktop app's updates: the newest release on GitHub, and installing it in place of the running
 * app. Only the desktop app has one.
 */
interface AppUpdater {
    /** The running app's version ("0.5.0"); null for a run from the sources, which is never out of date. */
    val currentVersion: String?

    /** The newest release when it is newer than the running app, else null. Throws when the releases cannot be read. */
    suspend fun check(): AppUpdate?

    /** Downloads the update's installer and checks it against its SHA-256; returns the file's path. [onProgress] goes from 0 to 1, null when the size is unknown. */
    suspend fun download(update: AppUpdate, onProgress: (Float?) -> Unit): String

    /** Starts installing the downloaded installer, which replaces this app and opens the new one, and ends this app. */
    suspend fun installAndRestart(installer: String)
}

/** Where the app stands with its updates. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val version: String?) : UpdateState
    data class Available(val update: AppUpdate) : UpdateState
    /** [progress] goes from 0 to 1, null when the size is unknown. */
    data class Downloading(val update: AppUpdate, val progress: Float?) : UpdateState
    data class Installing(val update: AppUpdate) : UpdateState
    data class Failed(val message: String, val update: AppUpdate?) : UpdateState
}

/**
 * Checks for a new release when the app starts (at most every [CHECK_EVERY], unless switched off in
 * the settings) or when asked to, and installs it on request. A version the user skipped is not
 * offered again at start, only when they check themselves.
 */
class AppUpdateService(
    private val updater: AppUpdater,
    private val settings: SettingsRepository,
    private val clock: Clock = Clock.System,
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** Whether the offer of an update was put away for this session. */
    private val _dismissed = MutableStateFlow(false)
    val dismissed: StateFlow<Boolean> = _dismissed.asStateFlow()

    val currentVersion: String? get() = updater.currentVersion

    /** The check the app makes when it starts. */
    suspend fun checkOnStart() {
        val s = settings.settings.first()
        if (!s.checkForUpdates || updater.currentVersion == null) return
        val now = clock.now().toEpochMilliseconds()
        if (now - s.lastUpdateCheck < CHECK_EVERY.inWholeMilliseconds) return
        val found = runCheck() ?: return
        if (found.version == s.skippedUpdate) _state.value = UpdateState.UpToDate(updater.currentVersion)
    }

    /** A check the user asked for: it offers even a version they skipped. */
    suspend fun check() {
        _dismissed.value = false
        runCheck()
    }

    private suspend fun runCheck(): AppUpdate? {
        _state.value = UpdateState.Checking
        return try {
            val update = updater.check()
            settings.update { it.copy(lastUpdateCheck = clock.now().toEpochMilliseconds()) }
            _state.value = if (update == null) UpdateState.UpToDate(updater.currentVersion) else UpdateState.Available(update)
            update
        } catch (e: Exception) {
            _state.value = UpdateState.Failed(e.message ?: "Could not check for updates", null)
            null
        }
    }

    /** Downloads the available update and installs it, which ends the app; a failure is reported in [state]. */
    suspend fun install() {
        val update = when (val s = _state.value) {
            is UpdateState.Available -> s.update
            is UpdateState.Failed -> s.update
            else -> null
        } ?: return
        if (update.installer == null) return
        try {
            _state.value = UpdateState.Downloading(update, 0f)
            val file = updater.download(update) { progress -> _state.value = UpdateState.Downloading(update, progress) }
            _state.value = UpdateState.Installing(update)
            updater.installAndRestart(file)
        } catch (e: Exception) {
            _state.value = UpdateState.Failed(e.message ?: "Could not install the update", update)
        }
    }

    /** Hides the offer until the app starts again. */
    fun later() {
        _dismissed.value = true
    }

    /** Never offers this version at start again. */
    suspend fun skip(update: AppUpdate) {
        settings.update { it.copy(skippedUpdate = update.version) }
        _dismissed.value = true
        _state.value = UpdateState.UpToDate(updater.currentVersion)
    }

    companion object {
        /** How often the app looks for a new release when it starts. */
        val CHECK_EVERY = 20.hours
    }
}

/** Release versions, "0.10.2", compared by their numbers; null for a text that is not one. */
object Versions {
    fun parse(text: String): List<Int>? = text.trim().removePrefix("v").split('.').takeIf { it.size == 3 }?.map { it.toIntOrNull() ?: return null }

    /** Whether [candidate] is a later version than [current]. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate) ?: return false
        val b = parse(current) ?: return true
        for (i in 0 until 3) if (a[i] != b[i]) return a[i] > b[i]
        return false
    }
}
