package com.idiomiko.core.domain.books.discover

/** A library's own word that it is down for maintenance, as opposed to a request that went wrong. */
class LibraryDownException(message: String) : IllegalStateException(message)

/** How a library answered when it was last asked. */
enum class LibraryStatus {
    CHECKING, AVAILABLE, DOWN_FOR_MAINTENANCE, UNREACHABLE;

    val isDown: Boolean get() = this == DOWN_FOR_MAINTENANCE || this == UNREACHABLE

    companion object {
        /** What a search that ended with [error], or without one, tells of its library. */
        fun of(error: Throwable?): LibraryStatus = when (error) {
            null -> AVAILABLE
            is LibraryDownException -> DOWN_FOR_MAINTENANCE
            else -> UNREACHABLE
        }
    }
}

/** A library's answer to a search: its first page of books, or the error that came instead. */
data class LibraryResult(val source: BookSource, val page: CatalogPage?, val error: Throwable?) {
    val status: LibraryStatus get() = LibraryStatus.of(error)
}

/** A book found in a library and, once it is judged, how much of it the reader knows. */
interface JudgedBook {
    val book: FoundBook
    val coverage: TextCoverage?
}

/** The kinds of book the reader can narrow the libraries' books to, each backed by what the libraries tell of a book. */
enum class Topic { SHORT_STORIES, CHILDRENS_BOOKS, POEMS, CLASSIC_NOVELS }

/** The rows of books picked for the reader from every library. */
enum class Shelf { EASY_WINS, CLASSICS_WITHIN_REACH }

/** A shelf's books, best first; those from [hardFrom] on are harder than the reader asked for. */
data class ShelfBooks<T>(val books: List<T>, val hardFrom: Int)

/** Books ranked for the reader: those they know enough of, best first, then those not judged yet; [harder] were left out. */
data class Ranked<T>(val books: List<T>, val harder: Int)

/**
 * How the books of every library are brought together for the reader: merged into one list, narrowed to a topic,
 * ranked by the share of their words the reader knows, and set out on shelves.
 */
object Discovery {
    /** The least shares known the How hard? scale stops at, hardest first; 0 takes every book. */
    val KNOWN_STOPS: List<Int> = listOf(0, 75, 80, 85, 90, 95)

    /** Where the scale starts: books the reader knows nine words in ten of. */
    const val DEFAULT_MIN_KNOWN = 90

    /** How far below the reader's level a classic is still within reach. */
    const val REACH = 15

    /** Up to this many running words a text is short, about half an hour's reading. */
    const val SHORT_WORDS = 2_000

    /** How many harder classics end their shelf, to show what lies ahead. */
    const val HARD_TAIL = 2

    /** Libraries of short books for children. */
    val CHILDREN_SOURCES: Set<BookSource> = setOf(BookSource.GDL, BookSource.STORYBOOKS, BookSource.STORYWEAVER)

    /** Libraries that name each book's genres, so poems and short stories can be told apart. */
    val GENRE_SOURCES: Set<BookSource> = setOf(BookSource.ADAMASTOR, BookSource.WOLNE_LEKTURY, BookSource.ELG, BookSource.BEN_YEHUDA)

    /** Libraries of literature: every one but the children's. */
    val CLASSIC_SOURCES: Set<BookSource> = BookSource.entries.toSet() - CHILDREN_SOURCES

    /** A short book: a children's book, or a text judged whole and found short. */
    fun isShort(book: FoundBook, coverage: TextCoverage?): Boolean =
        book.source in CHILDREN_SOURCES || (coverage?.wholeWords ?: Int.MAX_VALUE) <= SHORT_WORDS

    fun matches(topic: Topic, book: FoundBook, coverage: TextCoverage?): Boolean = when (topic) {
        Topic.SHORT_STORIES -> isShort(book, coverage) || book.hasGenre(SHORT_STORY_WORDS)
        Topic.CHILDRENS_BOOKS -> book.source in CHILDREN_SOURCES || book.hasGenre(CHILDREN_WORDS)
        Topic.POEMS -> book.hasGenre(POETRY_WORDS)
        Topic.CLASSIC_NOVELS -> book.source in CLASSIC_SOURCES && !isShort(book, coverage) && !book.hasGenre(POETRY_WORDS + CHILDREN_WORDS + SHORT_STORY_WORDS)
    }

    /** The topics the libraries of a language can tell apart; poems only where a library names genres. */
    fun topics(sources: Collection<BookSource>): List<Topic> = Topic.entries.filter { topic ->
        when (topic) {
            Topic.SHORT_STORIES, Topic.CHILDRENS_BOOKS -> sources.any { it in CHILDREN_SOURCES || it in GENRE_SOURCES }
            Topic.POEMS -> sources.any { it in GENRE_SOURCES }
            Topic.CLASSIC_NOVELS -> sources.any { it in CLASSIC_SOURCES }
        }
    }

    /**
     * The books of every library as one list, [bySource] in the libraries' order: the first of each, then the
     * second of each, so no library crowds out the others. A book several libraries have is listed once, the
     * copy already judged if there is one, else the first.
     */
    fun <T : JudgedBook> merge(bySource: List<List<T>>): List<T> {
        val longest = bySource.maxOfOrNull { it.size } ?: 0
        val interleaved = (0 until longest).flatMap { i -> bySource.mapNotNull { it.getOrNull(i) } }
        return interleaved.groupBy { identity(it.book) }.values.map { copies -> copies.firstOrNull { it.coverage != null } ?: copies.first() }
    }

    /** The books the reader knows at least [minKnown]% of, the best known first, then those not judged yet. */
    fun <T : JudgedBook> rank(books: List<T>, minKnown: Int): Ranked<T> {
        val judged = books.filter { it.coverage != null }
        val fit = judged.filter { it.percent >= minKnown }.sortedByDescending { it.percent }
        return Ranked(fit + books.filter { it.coverage == null }, judged.size - fit.size)
    }

    /**
     * A shelf's books. Easy wins: short books the reader knows at least [minKnown]% of. Classics within reach: the
     * other books of the literature libraries down to [REACH] below that, then the [HARD_TAIL] best known below.
     */
    fun <T : JudgedBook> shelf(shelf: Shelf, books: List<T>, minKnown: Int): ShelfBooks<T> {
        val easy = books.filter { it.coverage != null && isShort(it.book, it.coverage) && it.percent >= minKnown }.sortedByDescending { it.percent }
        if (shelf == Shelf.EASY_WINS) return ShelfBooks(easy, easy.size)
        val classics = books.filter { it.coverage != null && it.book.source in CLASSIC_SOURCES && it !in easy }.sortedByDescending { it.percent }
        val (reach, beyond) = classics.partition { it.percent >= minKnown - REACH }
        return ShelfBooks(reach + beyond.take(HARD_TAIL), reach.size)
    }

    private val JudgedBook.percent: Int get() = coverage?.percent ?: 0

    /** What makes two libraries' copies the same book: the title and the first author's surname, without case or marks. */
    private fun identity(book: FoundBook): String {
        fun plain(text: String) = text.lowercase().filter { it.isLetterOrDigit() }
        return plain(book.title) + "|" + plain(book.authors.firstOrNull()?.trim()?.substringAfterLast(' ').orEmpty())
    }

    private fun FoundBook.hasGenre(words: List<String>): Boolean = subjects.any { subject -> words.any { subject.contains(it, ignoreCase = true) } }

    /** Stems of the genre names the libraries give, in their languages. */
    private val POETRY_WORDS = listOf("poe", "poé", "poè", "lyri", "líri", "liry", "gedicht", "wiersz", "שיר")
    private val SHORT_STORY_WORDS = listOf("conto", "short stor", "nouvelle", "erzählung", "opowiada", "racconto", "cuento", "relato", "סיפור")
    private val CHILDREN_WORDS = listOf("infantil", "infanto", "child", "juvenil", "kinder", "jeunesse", "enfant", "dzieci", "dla dzieci", "ragazzi", "bambin", "ילדים")
}
