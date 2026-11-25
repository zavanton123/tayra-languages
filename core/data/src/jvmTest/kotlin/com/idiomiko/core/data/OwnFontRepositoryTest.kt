package com.idiomiko.core.data

import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.DatabaseMaintenanceImpl
import com.idiomiko.core.data.repository.OwnFontRepositoryImpl
import com.idiomiko.core.domain.fonts.OwnFont
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OwnFontRepositoryTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-fonts", ".db").also { it.delete() }))
    private val fonts = OwnFontRepositoryImpl(provider)
    private val font = ByteArray(2048) { (it % 251).toByte() }.also { it[0] = 0; it[1] = 1; it[2] = 0; it[3] = 0 }

    @Test
    fun aFontFileIsKeptListedAndRemoved() = runBlocking {
        val added = fonts.add("Zeta_Book-Regular.ttf", font)!!
        val other = fonts.add("Alpha.otf", font)!!
        assertEquals(listOf(OwnFont(other.id, "Alpha"), OwnFont(added.id, "Zeta Book")), fonts.fonts.first())
        assertContentEquals(font, fonts.data(added.id))

        fonts.remove(added.id)
        assertEquals(listOf(other), fonts.fonts.first())
        assertNull(fonts.data(added.id))
    }

    @Test
    fun aFileThatIsNoFontIsTurnedAway() = runBlocking {
        assertNull(fonts.add("notes.ttf", "not a font at all".encodeToByteArray()))
        assertEquals(emptyList(), fonts.fonts.first())
    }

    /** Clearing the learning data leaves the reader's fonts, as it leaves the settings. */
    @Test
    fun clearingTheDataKeepsTheFonts() = runBlocking {
        val added = fonts.add("Mine.ttf", font)!!
        DatabaseMaintenanceImpl(provider).wipeAllData()
        assertEquals(listOf(added), fonts.fonts.first())
    }
}
