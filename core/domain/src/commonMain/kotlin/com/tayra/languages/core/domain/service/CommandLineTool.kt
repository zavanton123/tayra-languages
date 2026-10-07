package com.tayra.languages.core.domain.service

/**
 * The `tayra` command that comes inside the desktop app, and putting it on the PATH so a terminal
 * finds it. Only the desktop app has one.
 */
interface CommandLineTool {
    /** False where this copy of the app has no `tayra` to put on the PATH. */
    val available: Boolean

    suspend fun status(): CommandLineStatus

    /** Puts `tayra` on the PATH of new terminals. */
    suspend fun install(): CommandLineStatus

    /** Takes it off again. */
    suspend fun uninstall(): CommandLineStatus
}

data class CommandLineStatus(
    /** Whether a new terminal finds `tayra`. */
    val installed: Boolean,
    /** The `tayra` a new terminal runs. */
    val location: String? = null,
    /** Whether it is the one this app put there, which it can take away again. */
    val removable: Boolean = false,
    /** The shell profile or setting that got the PATH entry, when the install changed one. */
    val pathChangedIn: String? = null,
    /** Why the last install or removal failed. */
    val error: String? = null,
)
