package com.tayra.languages.core.domain.books

import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.BookService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Installs and removes book packs. Installing adds the pack's books to the library of every
 * language it is in, each marked with the pack as its source; removing deletes those books again.
 */
class BookPackService(
    private val store: BookPackStore,
    private val books: BookRepository,
    private val bookService: BookService,
    private val languages: LanguageRepository,
) {
    private val _packs = MutableStateFlow(BookPacks.all.map { BookPackStatus(it, PackState.NotInstalled) })

    /** Every known pack with its current state, in catalog order. */
    val packs: StateFlow<List<BookPackStatus>> = _packs.asStateFlow()

    /**
     * Reads which packs are on the device; call once at start. A pack whose books are in the
     * library without its file (restored from a backup, say) counts as installed too, so it is
     * offered for removal rather than downloaded over its books.
     */
    suspend fun refresh() {
        for (pack in BookPacks.all) {
            if (stateOf(pack) is PackState.Downloading) continue
            val size = store.installedSize(pack)
            val state = when {
                size != null -> PackState.Installed(size)
                installedBookIds(pack).isNotEmpty() -> PackState.Installed(0)
                else -> PackState.NotInstalled
            }
            setState(pack, state)
        }
    }

    /** Downloads a pack and adds its books; failures end in [PackState.Failed] rather than an exception. */
    suspend fun download(pack: BookPack) {
        if (stateOf(pack) is PackState.Downloading) return
        setState(pack, PackState.Downloading(null))
        try {
            store.install(pack) { progress -> setState(pack, PackState.Downloading(progress)) }
            val packed = store.books(pack)
            if (packed.isEmpty()) {
                store.remove(pack)
                error("The downloaded file has no books this version of the app can read")
            }
            addBooks(pack, packed)
            setState(pack, PackState.Installed(store.installedSize(pack) ?: 0))
        } catch (e: Exception) {
            setState(pack, PackState.Failed(e.message ?: "Download failed"))
        }
    }

    /** Adds the pack's books to the library of each language with the pack's code, leaving out the ones already there. */
    suspend fun addBooks(pack: BookPack, packed: List<PackedBook>) {
        val present = books.getBooks().mapNotNull { it.sourceUri }.toSet()
        for (language in languages.getAll()) {
            if (LanguageCodes.codeFor(language.name) != pack.languageCode) continue
            for (book in packed) {
                val source = BookPacks.sourceOf(pack, book.id)
                if (source in present) continue
                bookService.create(BookDraft(languageId = language.id, title = book.title, text = book.text, sourceUri = source, tags = (listOf(book.level) + book.tags).distinct()))
            }
        }
    }

    /** Deletes the pack's books, with the reading progress and bookmarks in them, and then the file. */
    suspend fun remove(pack: BookPack) {
        for (id in installedBookIds(pack)) books.deleteBook(id)
        store.remove(pack)
        setState(pack, PackState.NotInstalled)
    }

    /** The library's books that came from [pack], whatever file brought them. */
    private suspend fun installedBookIds(pack: BookPack): List<Long> =
        books.getBooks().filter { it.sourceUri?.startsWith("${BookPacks.SOURCE_PREFIX}${pack.id}/") == true }.map { it.id }

    private fun stateOf(pack: BookPack): PackState? = _packs.value.firstOrNull { it.pack.id == pack.id }?.state

    private fun setState(pack: BookPack, state: PackState) {
        _packs.update { list -> list.map { if (it.pack.id == pack.id) it.copy(state = state) else it } }
    }
}
