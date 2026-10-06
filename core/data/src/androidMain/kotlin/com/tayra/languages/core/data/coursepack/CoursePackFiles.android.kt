package com.tayra.languages.core.data.coursepack

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.tayra.languages.core.data.dictionary.DictionaryDownloader
import com.tayra.languages.core.data.dictionary.PackFiles
import com.tayra.languages.core.domain.courses.CoursePack
import java.io.File

actual class CoursePackFiles(private val context: Context, downloader: DictionaryDownloader) {
    private val files = PackFiles(File(context.filesDir, "course-packs"), downloader)

    actual suspend fun installedSize(pack: CoursePack): Long? = files.installedSize(pack.id, CoursePack.FORMAT)

    actual suspend fun install(pack: CoursePack, onProgress: (Float?) -> Unit) = files.install(pack.id, pack.url, CoursePack.FORMAT, onProgress)

    actual suspend fun remove(pack: CoursePack) = files.remove(pack.id)

    actual suspend fun openDriver(pack: CoursePack): SqlDriver? {
        if (files.installedSize(pack.id, CoursePack.FORMAT) == null) return null
        // The file's user_version equals the schema version, so the driver neither creates nor migrates.
        return AndroidSqliteDriver(schema = CoursePackDatabase.Schema.synchronous(), context = context, name = files.file(pack.id).absolutePath)
    }
}
