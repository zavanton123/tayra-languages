package com.tayra.languages.core.data.coursepack

import app.cash.sqldelight.db.SqlDriver
import com.tayra.languages.core.domain.courses.CoursePack

/** Platform storage for downloaded course packs, which also opens them as read-only SQLite drivers. */
expect class CoursePackFiles {
    /** Size of the installed pack in bytes, or null when it is not installed. */
    suspend fun installedSize(pack: CoursePack): Long?
    suspend fun install(pack: CoursePack, onProgress: (Float?) -> Unit)
    suspend fun remove(pack: CoursePack)
    /** A driver over the installed pack, or null when it is not installed. */
    suspend fun openDriver(pack: CoursePack): SqlDriver?
}
