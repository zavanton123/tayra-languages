package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.domain.books.BookPack
import com.tayra.languages.core.domain.books.BookPackService
import com.tayra.languages.core.domain.books.BookPackStore
import com.tayra.languages.core.domain.books.BookPacks
import com.tayra.languages.core.domain.books.PackedBook
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookService
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A book pack's books join the library of its language once, marked with the pack, and leave with it. */
class BookPackServiceTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-book-pack", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val bookService = BookService(books, languages)
    private val pack = BookPacks.all.first { it.languageCode == "pt" }
    private val packed = listOf(
        PackedBook("pt-one", "A2", "Bichos da mata", "Wikilivros", "Animals.", "https://pt.wikibooks.org/wiki/Bichos_da_mata", "CC BY-SA 4.0", listOf("animals"), "A anta é o maior mamífero do Brasil."),
        PackedBook("pt-two", "C1", "Dom Casmurro", "Machado de Assis", "A novel.", "http://machado.mec.gov.br/", "Public domain", listOf("novel"), "Uma noite destas, vindo da cidade para o Engenho Novo."),
    )

    /** A store whose only pack is the Portuguese one, held in memory. */
    private inner class FakeStore(var fail: Boolean = false) : BookPackStore {
        var installed = false
        override suspend fun installedSize(pack: BookPack): Long? = if (installed) 2_048L else null
        override suspend fun install(pack: BookPack, onProgress: (Float?) -> Unit) {
            if (fail) error("Server answered 404")
            onProgress(1f)
            installed = true
        }
        override suspend fun remove(pack: BookPack) { installed = false }
        override suspend fun books(pack: BookPack): List<PackedBook> = if (installed) packed else emptyList()
    }

    @Test
    fun downloadingAddsTheBooksToTheLanguageOnceAndRemovingTakesThemOut() = runBlocking {
        val portuguese = languages.save(Language(name = "Portuguese"))
        val spanish = languages.save(Language(name = "Spanish"))
        val own = bookService.create(BookDraft(languageId = portuguese, title = "Meu texto", text = "Olá."))
        val store = FakeStore()
        val service = BookPackService(store, books, bookService, languages)
        service.refresh()
        assertEquals(PackState.NotInstalled, service.packs.value.single { it.pack.id == pack.id }.state)

        service.download(pack)
        assertEquals(PackState.Installed(2_048L), service.packs.value.single { it.pack.id == pack.id }.state)
        val added = books.getBooks().filter { it.sourceUri?.startsWith(BookPacks.SOURCE_PREFIX) == true }
        assertEquals(listOf("Bichos da mata", "Dom Casmurro"), added.map { it.title })
        assertTrue(added.all { it.languageId == portuguese }, "the books go to the language of the pack, not to Spanish ($spanish)")
        assertEquals(listOf("A2", "animals"), added.first().tags)
        // Downloaded again (the file lost, say), the books already in the library are not doubled.
        service.addBooks(pack, packed)
        assertEquals(2, books.getBooks().count { it.sourceUri?.startsWith(BookPacks.SOURCE_PREFIX) == true })

        // Without its file, a pack whose books are in the library still counts as installed.
        store.installed = false
        service.refresh()
        assertEquals(PackState.Installed(0), service.packs.value.single { it.pack.id == pack.id }.state)

        service.remove(pack)
        assertEquals(PackState.NotInstalled, service.packs.value.single { it.pack.id == pack.id }.state)
        assertEquals(listOf(own), books.getBooks().map { it.id }, "the reader's own book stays")
    }

    @Test
    fun aFailedDownloadIsReportedAndLeavesNoBooks() = runBlocking {
        languages.save(Language(name = "Portuguese"))
        val service = BookPackService(FakeStore(fail = true), books, bookService, languages)
        service.download(pack)
        val state = service.packs.value.single { it.pack.id == pack.id }.state
        assertTrue(state is PackState.Failed && state.message == "Server answered 404", "$state")
        assertEquals(emptyList(), books.getBooks())
    }
}
