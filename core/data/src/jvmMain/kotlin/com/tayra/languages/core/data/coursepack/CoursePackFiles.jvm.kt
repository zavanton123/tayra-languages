package com.tayra.languages.core.data.coursepack

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.dictionary.DictionaryDownloader
import com.tayra.languages.core.data.dictionary.PackFiles
import com.tayra.languages.core.domain.courses.CoursePack
import java.io.File
import java.util.Properties

actual class CoursePackFiles(
    downloader: DictionaryDownloader,
    directory: File = File(DatabaseDriverFactory.dataDirectory(), "course-packs"),
) {
    private val files = PackFiles(directory, downloader)

    actual suspend fun installedSize(pack: CoursePack): Long? = files.installedSize(pack.id, CoursePack.FORMAT)

    actual suspend fun install(pack: CoursePack, onProgress: (Float?) -> Unit) = files.install(pack.id, pack.url, CoursePack.FORMAT, onProgress)

    actual suspend fun remove(pack: CoursePack) = files.remove(pack.id)

    actual suspend fun openDriver(pack: CoursePack): SqlDriver? {
        if (files.installedSize(pack.id, CoursePack.FORMAT) == null) return null
        val properties = Properties().apply { put("open_mode", "1") } // SQLITE_OPEN_READONLY
        return JdbcSqliteDriver("jdbc:sqlite:${files.file(pack.id).absolutePath}", properties)
    }
}
