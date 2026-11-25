package com.idiomiko.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.db.databaseDispatcher
import com.idiomiko.core.domain.fonts.FontFiles
import com.idiomiko.core.domain.fonts.OwnFont
import com.idiomiko.core.domain.fonts.OwnFontRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class OwnFontRepositoryImpl(private val provider: DatabaseProvider, private val clock: Clock = Clock.System) : OwnFontRepository {
    override val fonts: Flow<List<OwnFont>> = flow {
        provider.database().ownFontsQueries.listFonts().asFlow().mapToList(databaseDispatcher).collect { rows ->
            emit(rows.map { OwnFont(it.id, it.name) })
        }
    }

    override suspend fun add(fileName: String, bytes: ByteArray): OwnFont? {
        if (!FontFiles.isFont(bytes)) return null
        val name = FontFiles.nameOf(fileName)
        val id = withContext(databaseDispatcher) {
            val q = provider.database().ownFontsQueries
            q.transactionWithResult {
                q.insertFont(name, bytes, clock.now().toEpochMilliseconds())
                q.lastFontId().awaitAsOne()
            }
        }
        return OwnFont(id, name)
    }

    override suspend fun remove(id: Long) {
        withContext(databaseDispatcher) { provider.database().ownFontsQueries.deleteFont(id) }
    }

    override suspend fun data(id: Long): ByteArray? =
        withContext(databaseDispatcher) { provider.database().ownFontsQueries.fontData(id).awaitAsOneOrNull() }
}
